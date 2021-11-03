package eu.outerheaven.certmanager.controller.service

import eu.outerheaven.certmanager.controller.dto.CertificateDto
import eu.outerheaven.certmanager.controller.dto.CertificateImportDto
import eu.outerheaven.certmanager.controller.entity.Certificate
import eu.outerheaven.certmanager.controller.entity.Instance
import eu.outerheaven.certmanager.controller.entity.Keystore
import eu.outerheaven.certmanager.controller.form.CertificateFormGUI
import eu.outerheaven.certmanager.controller.form.KeystoreForm
import eu.outerheaven.certmanager.controller.form.KeystoreFormGUI
import eu.outerheaven.certmanager.controller.repository.CertificateRepository
import eu.outerheaven.certmanager.controller.repository.InstanceRepository
import eu.outerheaven.certmanager.controller.repository.KeystoreRepository
import eu.outerheaven.certmanager.controller.util.CertificateLoader
import eu.outerheaven.certmanager.controller.util.PreparedRequest
import eu.outerheaven.certmanager.controller.util.deserializers.X509CertificateDeserializer
import org.apache.tomcat.util.http.fileupload.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

import javax.servlet.http.HttpServletRequest
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class CertificateService {

    private final Logger LOG = LoggerFactory.getLogger(CertificateService)

    @Autowired
    private final KeystoreRepository keystoreRepository

    @Autowired
    private final InstanceRepository instanceRepository

    @Autowired
    private final CertificateRepository repository

    String api_url="/api/certificate"

    CertificateFormGUI toFormGUI(Certificate certificate){
        Keystore keystore = keystoreRepository.findById(certificate.getKeystoreId()).get()
        Instance instance = instanceRepository.findById(keystore.getInstanceId()).get()
        String status = certStatus(certificate.getX509Certificate().getNotBefore(), certificate.getX509Certificate().getNotAfter())
        Boolean pk=false
        if(certificate.getPrivateKey() != null){
            pk=true
        }
        CertificateFormGUI certificateFormGUI = new CertificateFormGUI(
                id: certificate.id,
                alias: certificate.alias,
                keystorePath: keystore.location,
                instanceName: instance.name,
                hostname: instance.hostname,
                managed: certificate.managed,
                subject: certificate.getX509Certificate().subjectDN,
                issuer: certificate.getX509Certificate().issuerDN,
                validFrom: certificate.getX509Certificate().notBefore,
                validTo: certificate.getX509Certificate().notAfter,
                serial: certificate.getX509Certificate().serialNumber,
                status: status,
                privateKey: pk
        )
        return certificateFormGUI
    }

    ArrayList<CertificateFormGUI> toFormGUI(ArrayList<Certificate> certificates){
        ArrayList<CertificateFormGUI> certificateFormGUIS = new ArrayList<>()
        certificates.forEach(r ->certificateFormGUIS.add(toFormGUI(r)))
        return certificateFormGUIS
    }

    ArrayList<CertificateFormGUI> getAllGUI(){
        ArrayList<Certificate> certificates = repository.findAll() as ArrayList<Certificate>
        ArrayList<CertificateFormGUI> certificateFormGUIS = toFormGUI(certificates)
        return certificateFormGUIS
    }

    void fetchCert(){
        /*PreparedRequest preparedRequest = new PreparedRequest()
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<Keystore> request = new HttpEntity<>(keystore, preparedRequest.getHeader(instance))



        Keystore keystore = restTemplate.getForObject("http://192.168.1.23:8100/api/keystore" + "/1", Keystore.class);
        restTemplate.getForObject("http://192.168.1.23:8100/api/keystore" + "/1",Keystore.class,request)
        ResponseEntity<Keystore> response = restTemplate.exchange(
                "http://192.168.1.23:8100/api/keystore" + "/1",
                HttpMethod.POST,
                request,
                Keystore.class
        );

         */

    }

    String certStatus(Date notBefore, Date notAfter){
        String status
        Instant instant = Instant.ofEpochMilli(notBefore.getTime())
        LocalDateTime ldtNotBefore = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        instant = Instant.ofEpochMilli(notAfter.getTime())
        LocalDateTime ldtNotAfter = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        //IF the not before date is after current time, invalid cert
        //TODO optimise order of this
        if(ldtNotBefore.isAfter(LocalDateTime.now())){
            status="NOT YET VALID"
        } else if(ldtNotAfter.isBefore(LocalDateTime.now())){
            //IF not after is before the current date, it is expired
            status="EXPIRED"
        }else if(ldtNotAfter.plusDays(30).isBefore(LocalDateTime.now())){
            //IF not after +30 days is before it will expire soon
            status="EXPIRING SOON"
        }else(status="VALID")

        return status
    }

    Resource exportAsPem(Long certificateId){
        CertificateLoader certificateLoader = new CertificateLoader()
        String folderName = certificateLoader.generateRandomName()
        Path path = Paths.get(folderName)
        while (Files.exists(path)){
            folderName = certificateLoader.generateRandomName()
            path=Paths.get(folderName)
        }
        try{
            new File(path.toString()).mkdirs()
            Certificate certificate = repository.findById(certificateId).get()
            String filename =""
            if(certificate.getAlias() == null){
                filename ="certificate.cer"
            }else {
                filename =certificate.getAlias() + ".cer"
            }
            filename = filename.replaceAll("[^\\dA-Za-z. ]", "").replaceAll("\\s+", "_")
            certificateLoader.writeCertToFileBase64Encoded(certificate.getX509Certificate(),folderName + "/" + filename)
            if(certificate.privateKey != null){
                certificateLoader.writeKeyToFileBase64Encoded(certificate.getPrivateKey(), folderName + "/" + filename)
            }
            String fullPath =  "./" + folderName + "/" + filename
            LOG.info("Path to the file is " + fullPath)
            path = Paths.get(folderName + "/" + filename)
            Resource resource = new UrlResource(path.toUri())
            return resource
        }catch(Exception exception){
            LOG.error("Failed exporting certificate:" + exception)
        }
    }

    void deleteTempFile(Resource resource){
        Path path = Paths.get(resource.getURI())
        FileUtils.deleteDirectory(path.getParent().toFile())
    }

    void importCertificate(CertificateImportDto certificateImportDto){
        if(certificateImportDto.getImportFormat() == "PEM"){
            CertificateLoader certificateLoader = new CertificateLoader()
            certificateLoader.decodeImportPem(certificateImportDto.getBase64File(), certificateImportDto.getFilename())
        }else {
            LOG.info("Well fuck seems like the developer hasnt implemented this yet")
        }
    }

    void propCertToAgents(List<Certificate> certificates, List<KeystoreFormGUI> keystoreFormGUIS){

    }

}
