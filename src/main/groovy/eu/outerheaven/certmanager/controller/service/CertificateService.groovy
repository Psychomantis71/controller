package eu.outerheaven.certmanager.controller.service

import eu.outerheaven.certmanager.controller.dto.CertificateDto
import eu.outerheaven.certmanager.controller.dto.RetrieveFromPortDto
import eu.outerheaven.certmanager.controller.entity.Certificate
import eu.outerheaven.certmanager.controller.entity.Instance
import eu.outerheaven.certmanager.controller.entity.KeystoreCertificate
import eu.outerheaven.certmanager.controller.form.CertificateFormGUI
import eu.outerheaven.certmanager.controller.form.RetrieveFromPortForm
import eu.outerheaven.certmanager.controller.repository.CertificateRepository
import eu.outerheaven.certmanager.controller.repository.InstanceRepository
import eu.outerheaven.certmanager.controller.util.CertificateLoader
import eu.outerheaven.certmanager.controller.util.PreparedRequest
import org.apache.tomcat.util.http.fileupload.FileUtils
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.JCEECPublicKey
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.env.Environment
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.PublicKey
import java.security.cert.CertificateParsingException
import java.security.cert.X509Certificate
import java.security.interfaces.DSAPublicKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.logging.FileHandler
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Service
class CertificateService {

    String api_url="/api/certificate"

    private final Logger LOG = LoggerFactory.getLogger(CertificateService)

    @Autowired
    private final CertificateRepository repository

    @Autowired
    private final Environment environment

    @Autowired
    private final InstanceRepository instanceRepository

    CertificateFormGUI toFormGUI(Certificate certificate){
        String certstat = certStatus(certificate.x509Certificate.notBefore,certificate.x509Certificate.notAfter)
        boolean pk = false
        if(certificate.privateKey != null) pk=true
        CertificateFormGUI certificateFormGUI = new CertificateFormGUI(
                id: certificate.id,
                status: certstat,
                subject: certificate.x509Certificate.subjectDN,
                issuer: certificate.x509Certificate.issuerDN,
                validFrom: certificate.x509Certificate.notBefore,
                validTo: certificate.x509Certificate.notAfter,
                serial: certificate.x509Certificate.serialNumber,
                signature: certificate.x509Certificate.publicKey.getAlgorithm(),
                signatureHashAlgorithm: certificate.x509Certificate.getSigAlgName().toString(),
                keysize: getKeyLength(certificate.x509Certificate.publicKey),
                keyUsage: getKeyUsageList(certificate.x509Certificate),
                enhancedKeyUsage: certificate.x509Certificate.getExtendedKeyUsage(),
                alternativeNameDNS: getAlternateNames(certificate.x509Certificate,2),
                alternativeNameIP: getAlternateNames(certificate.x509Certificate,7),
                basicConstraints: certificate.x509Certificate.basicConstraints,
                managed: certificate.managed,
                signerId: certificate.signerCertificateId,
                privateKey: pk
        )
        return certificateFormGUI
    }

    List<CertificateFormGUI> toFormGUI(List<Certificate> certificates){
        List<CertificateFormGUI> certificateFormGUIS = new ArrayList<>()
        certificates.forEach(r->{
            certificateFormGUIS.add(toFormGUI(r))
        })
        return certificateFormGUIS
    }

    List<CertificateFormGUI> getAllGUI(){
        List<Certificate> certificates = repository.findAll() as ArrayList<Certificate>
        List<CertificateFormGUI> certificateFormGUIS = toFormGUI(certificates)
        return certificateFormGUIS
    }

    String certStatus(Date notBefore, Date notAfter){
        String status
        Instant instant = Instant.ofEpochMilli(notBefore.getTime())
        LocalDateTime ldtNotBefore = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        instant = Instant.ofEpochMilli(notAfter.getTime())
        LocalDateTime ldtNotAfter = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        if(ldtNotBefore.isAfter(LocalDateTime.now())){
            status="NOT YET VALID"
        } else if(ldtNotAfter.isBefore(LocalDateTime.now())){
            //IF not after is before the current date, it is expired
            status="EXPIRED"
        }else if(ldtNotAfter.plusDays(environment.getProperty("controller.expiration.check.warn.period").toInteger()).isBefore(LocalDateTime.now())){
            status="EXPIRING SOON"
        }else(status="VALID")

        return status
    }

    protected String getAlternateNames(final X509Certificate cert, Integer targetType) {
        final StringBuilder res = new StringBuilder();

        try {
            if (cert.getSubjectAlternativeNames() == null) {
                return null;
            }

            for (List<?> entry : cert.getSubjectAlternativeNames()) {
                final int type = ((Integer) entry.get(0)).intValue();

                // DNS or IP
                if (type == targetType) {
                    if (res.length() > 0) {
                        res.append(", ");
                    }

                    res.append(entry.get(1));
                }
            }
        } catch (CertificateParsingException ex) {
            // Do nothing
        }

        return res.toString();
    }

    //TODO ask someone for a more efficient way to do this, this is stupid
    List<String> getKeyUsageList(X509Certificate certificate){
        List<String> keyUsageList = new ArrayList<>()
        boolean[] keyUsage = certificate.getKeyUsage()
        if(keyUsage == null) return keyUsageList
        if(keyUsage[0]) keyUsageList.add(new String("digitalSignature"))
        if(keyUsage[1]) keyUsageList.add(new String("nonRepudiation"))
        if(keyUsage[2]) keyUsageList.add(new String("keyEncipherment"))
        if(keyUsage[3]) keyUsageList.add(new String("dataEncipherment"))
        if(keyUsage[4]) keyUsageList.add(new String("keyAgreement"))
        if(keyUsage[5]) keyUsageList.add(new String("keyCertSign"))
        if(keyUsage[6]) keyUsageList.add(new String("cRLSign"))
        if(keyUsage[7]) keyUsageList.add(new String("encipherOnly"))
        if(keyUsage[8]) keyUsageList.add(new String("decipherOnly"))

        return keyUsageList
    }

    /**
     * Gets the key length of supported keys
     * @param pk PublicKey used to derive the keysize
     * @return -1 if key is unsupported, otherwise a number >= 0. 0 usually means the length can not be calculated,
     * for example if the key is an EC key and the "implicitlyCA" encoding is used.
     */
    static int getKeyLength(final PublicKey pk) {
        int len = -1;
        if (pk instanceof RSAPublicKey) {
            final RSAPublicKey rsapub = (RSAPublicKey) pk;
            len = rsapub.getModulus().bitLength();
        } else if (pk instanceof JCEECPublicKey) {
            final JCEECPublicKey ecpriv = (JCEECPublicKey) pk;
            final org.bouncycastle.jce.spec.ECParameterSpec spec = ecpriv.getParameters();
            if (spec != null) {
                len = spec.getN().bitLength();
            } else {
                // We support the key, but we don't know the key length
                len = 0;
            }
        } else if (pk instanceof ECPublicKey) {
            final ECPublicKey ecpriv = (ECPublicKey) pk;
            final java.security.spec.ECParameterSpec spec = ecpriv.getParameters();
            if (spec != null) {
                len = spec.getOrder().bitLength(); // does this really return something we expect?
            } else {
                // We support the key, but we don't know the key length
                len = 0;
            }
        } else if (pk instanceof DSAPublicKey) {
            final DSAPublicKey dsapub = (DSAPublicKey) pk;
            if ( dsapub.getParams() != null ) {
                len = dsapub.getParams().getP().bitLength();
            } else {
                len = dsapub.getY().bitLength();
            }
        }
        return len;
    }

    void getExtendedKeyUsageList(X509Certificate certificate){
        /*
        The following extended key usage purposes are defined by RFC 3280:

        serverAuth (1.3.6.1.5.5.7.3.1) -- TLS Web server authentication
        clientAuth (1.3.6.1.5.5.7.3.2) -- TLS Web client authentication
        codeSigning (1.3.6.1.5.5.7.3.3) -- Code signing
        emailProtection (1.3.6.1.5.5.7.3.4) -- E-mail protection
        timeStamping (1.3.6.1.5.5.7.3.8) -- Timestamping
        ocspSigning (1.3.6.1.5.5.7.3.9) -- OCSPstamping

        The following purposes have been included in a predecessor draft of RFC 3280 and therefore continue to be registrated by this implementation:

        ipsecEndSystem (1.3.6.1.5.5.7.3.5) -- IP security end system
        ipsecTunnel (1.3.6.1.5.5.7.3.6) -- IP security tunnel termination
        ipsecUser (1.3.6.1.5.5.7.3.7) -- IP security user
        */


    }


    List<Certificate> retrieveFromAgent(RetrieveFromPortForm retrieveFromPortForm){
        try{
            Instance instance = instanceRepository.findById(retrieveFromPortForm.instanceId).get()
            PreparedRequest preparedRequest = new PreparedRequest()
            RestTemplate restTemplate = new RestTemplate();
            RetrieveFromPortDto retrieveFromPortDto = new RetrieveFromPortDto(
                    hostname: retrieveFromPortForm.hostname,
                    port: retrieveFromPortForm.port
            )
            HttpEntity<RetrieveFromPortDto> request = new HttpEntity<>(retrieveFromPortDto, preparedRequest.getHeader(instance))

            ResponseEntity<List<CertificateDto>> response = restTemplate.exchange(
                    instance.getAccessUrl() + api_url + "/retrieve-from-port",
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<List<CertificateDto>>(){}
            )
            List<CertificateDto> responseForms = response.getBody()
            LOG.info("Retrieved something from port")
            List<Certificate> certificates = toClass(responseForms)
            return certificates
        } catch(Exception e){
            LOG.error("Failed to retrieve something from port" + e )
        }

    }

    Resource exportAsPem(Certificate certificate, String filename){
        CertificateLoader certificateLoader = new CertificateLoader()
        String folderName = certificateLoader.generateRandomName()
        Path path = Paths.get(folderName)
        while (Files.exists(path)){
            folderName = certificateLoader.generateRandomName()
            path=Paths.get(folderName)
        }
        try{
            new File(path.toString()).mkdirs()
            certificateLoader.writeCertToFileBase64Encoded(certificate.getX509Certificate(),folderName + "/" + filename)
            if(certificate.privateKey != null){
                certificateLoader.writeKeyToFileBase64Encoded(certificate.getPrivateKey(), folderName + "/" + filename)
            }
            String fullPath =  "./" + folderName + "/" + filename
            LOG.info("Path to the file is " + fullPath)
            path = Paths.get(folderName + "/" + filename)
            File file = new File(folderName + "/" + filename)
            byte[] fileContent = Files.readAllBytes(file.toPath())

            //Resource resource = new UrlResource(path.toUri())
            Resource resource = new ByteArrayResource(fileContent)
            FileUtils.deleteDirectory(path.getParent().toFile())
            return resource
        }catch(Exception exception){
            LOG.error("Failed exporting certificate:" + exception)
        }
    }


    Resource exportAsZip(List<Certificate> certificates){
        CertificateLoader certificateLoader = new CertificateLoader()
        String folderName = certificateLoader.generateRandomName()
        Path path = Paths.get(folderName)
        while (Files.exists(path)){
            folderName = certificateLoader.generateRandomName()
            path=Paths.get(folderName)
        }
        try{
            new File(path.toString()).mkdirs()

            certificates.forEach( certificate -> {
                certificateLoader.writeCertToFileBase64Encoded(certificate.getX509Certificate(),folderName + "/certificate" + certificate.id.toString() +".cer" )
                if(certificate.privateKey != null){
                    certificateLoader.writeKeyToFileBase64Encoded(certificate.getPrivateKey(), folderName + "/certificate" + certificate.id.toString() +".cer" )
                }
            })
            zipFolder(new File("./" + folderName), new File("./" + folderName + ".zip"))

            //String fullPath =  "./" + folderName + "/" + filename
            //LOG.info("Path to the file is " + fullPath)
            path = Paths.get(folderName + "/" + "certificate0")

            File file = new File(folderName + ".zip")
            byte[] fileContent = Files.readAllBytes(file.toPath())

            //Resource resource = new UrlResource(path.toUri())
            Resource resource = new ByteArrayResource(fileContent)
            FileUtils.deleteDirectory(path.getParent().toFile())
            FileUtils.forceDelete(file)
            return resource
        }catch(Exception exception){
            LOG.error("Failed exporting zip:" + exception)
        }
    }

    void zipFolder(File srcFolder, File destZipFile) throws Exception {
        try (FileOutputStream fileWriter = new FileOutputStream(destZipFile);
             ZipOutputStream zip = new ZipOutputStream(fileWriter)) {

            addFolderToZip(srcFolder, srcFolder, zip);
        }
    }

    private void addFileToZip(File rootPath, File srcFile, ZipOutputStream zip) throws Exception {

        if (srcFile.isDirectory()) {
            addFolderToZip(rootPath, srcFile, zip);
        } else {
            byte[] buf = new byte[1024];
            int len;
            try ( FileInputStream input = new FileInputStream(srcFile) ) {
                String name = srcFile.getPath();
                name = name.replace(rootPath.getPath(), "");
                System.out.println("Zip " + srcFile + "\n to " + name);
                zip.putNextEntry(new ZipEntry(name));
                while ((len = input.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }
            }
        }
    }

    private void addFolderToZip(File rootPath, File srcFolder, ZipOutputStream zip) throws Exception {
        for (File fileName : srcFolder.listFiles()) {
            addFileToZip(rootPath, fileName, zip);
        }
    }


    //Frontend needs some key values to sort by, only for retrieve from port function!
    List<Certificate> assignIds(List<Certificate> certificates){
        for(Integer i=0; i<certificates.size();i++){
            certificates.get(i).setId(i)
        }
        return certificates
    }

    Certificate toClass(CertificateDto certificateDto){
        CertificateLoader certificateLoader = new CertificateLoader()
        Certificate certificate = new Certificate(
                x509Certificate: certificateLoader.decodeX509(certificateDto.encodedX509Certificate),
                privateKey: certificateLoader.decodeKey(certificateDto.encodedPrivateKey),

        )
        return certificate
    }
    List<Certificate> toClass(List<CertificateDto> certificateDtos){
        List<Certificate> certificates = new ArrayList<>()
        certificateDtos.forEach(r->{
            certificates.add(toClass(r))
        })
        return certificates
    }


}
