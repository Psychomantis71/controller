package eu.outerheaven.certmanager.controller.service

import com.sun.corba.se.impl.oa.poa.ActiveObjectMap
import eu.outerheaven.certmanager.controller.dto.CertificateDto
import eu.outerheaven.certmanager.controller.dto.CertificateImportDto
import eu.outerheaven.certmanager.controller.dto.KeystoreCertificateDto
import eu.outerheaven.certmanager.controller.entity.CaCertificate
import eu.outerheaven.certmanager.controller.entity.Certificate
import eu.outerheaven.certmanager.controller.entity.Instance
import eu.outerheaven.certmanager.controller.entity.Keystore
import eu.outerheaven.certmanager.controller.entity.KeystoreCertificate
import eu.outerheaven.certmanager.controller.form.CertificateFormGUI
import eu.outerheaven.certmanager.controller.form.KeystoreFormGUI
import eu.outerheaven.certmanager.controller.repository.CaCertificateRepository
import eu.outerheaven.certmanager.controller.repository.CertificateRepository
import eu.outerheaven.certmanager.controller.repository.InstanceRepository
import eu.outerheaven.certmanager.controller.repository.KeystoreCertificateRepository
import eu.outerheaven.certmanager.controller.repository.KeystoreRepository
import eu.outerheaven.certmanager.controller.util.CertificateLoader
import eu.outerheaven.certmanager.controller.util.PreparedRequest
import org.apache.tomcat.util.http.fileupload.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class KeystoreCertificateService {

    private final Logger LOG = LoggerFactory.getLogger(KeystoreCertificateService)

    @Autowired
    private final KeystoreRepository keystoreRepository

    @Autowired
    private final InstanceRepository instanceRepository

    @Autowired
    private final CertificateRepository certificateRepository

    @Autowired
    private final CaCertificateRepository caCertificateRepository

    @Autowired
    private final KeystoreCertificateRepository repository

    @Autowired
    private Environment environment

    String api_url="/api/certificate"

    //Refactored
    CertificateFormGUI toFormGUI(KeystoreCertificate certificate){
        Keystore keystore = keystoreRepository.findById(certificate.getKeystoreId()).get()
        Instance instance = instanceRepository.findById(keystore.getInstance().getId()).get()
        String status = certStatus(certificate.getCertificate().getX509Certificate().getNotBefore(), certificate.getCertificate().getX509Certificate().getNotAfter())
        Boolean pk=false
        if(certificate.getCertificate().getPrivateKey() != null){
            pk=true
        }
        CertificateFormGUI certificateFormGUI = new CertificateFormGUI(
                id: certificate.id,
                alias: certificate.alias,
                keystorePath: keystore.location,
                instanceName: instance.name,
                hostname: instance.hostname,
                managed: certificate.getCertificate().managed,
                subject: certificate.getCertificate().getX509Certificate().subjectDN,
                issuer: certificate.getCertificate().getX509Certificate().issuerDN,
                validFrom: certificate.getCertificate().getX509Certificate().notBefore,
                validTo: certificate.getCertificate().getX509Certificate().notAfter,
                serial: certificate.getCertificate().getX509Certificate().serialNumber,
                status: status,
                privateKey: pk
        )
        return certificateFormGUI
    }
    //Refactored
    ArrayList<CertificateFormGUI> toFormGUI(ArrayList<KeystoreCertificate> certificates){
        ArrayList<CertificateFormGUI> certificateFormGUIS = new ArrayList<>()
        certificates.forEach(r ->certificateFormGUIS.add(toFormGUI(r)))
        return certificateFormGUIS
    }
    //Refactored
    ArrayList<CertificateFormGUI> getAllGUI(){
        ArrayList<KeystoreCertificate> certificates = repository.findAll() as ArrayList<KeystoreCertificate>
        ArrayList<CertificateFormGUI> certificateFormGUIS = toFormGUI(certificates)
        return certificateFormGUIS
    }
    //TODO this needs to fetch parameter from config file
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

    String getCleanCertName(Long certificateId){
        KeystoreCertificate certificate = repository.findById(certificateId).get()
        String filename =""
        if(certificate.getAlias() == null){
            filename ="certificate.cer"
        }else {
            filename =certificate.getAlias() + ".cer"
        }
        filename = filename.replaceAll("[^\\dA-Za-z. ]", "").replaceAll("\\s+", "_")

        return filename
    }

    Resource exportAsPem(Long certificateId, String filename){
        CertificateLoader certificateLoader = new CertificateLoader()
        String folderName = certificateLoader.generateRandomName()
        Path path = Paths.get(folderName)
        while (Files.exists(path)){
            folderName = certificateLoader.generateRandomName()
            path=Paths.get(folderName)
        }
        try{
            new File(path.toString()).mkdirs()
            KeystoreCertificate certificate = repository.findById(certificateId).get()
            certificateLoader.writeCertToFileBase64Encoded(certificate.getCertificate().getX509Certificate(),folderName + "/" + filename)
            if(certificate.getCertificate().privateKey != null){
                certificateLoader.writeKeyToFileBase64Encoded(certificate.getCertificate().getPrivateKey(), folderName + "/" + filename)
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

    void importCertificate(CertificateImportDto certificateImportDto){
        List<KeystoreCertificate> keystoreCertificates = new ArrayList<>()
        CertificateLoader certificateLoader = new CertificateLoader()
        if(certificateImportDto.getImportFormat() == "PEM"){
            List<Certificate> certificates
            certificates = certificateLoader.decodeImportPem(certificateImportDto.getBase64File(), certificateImportDto.getFilename())

            for(int i=0;i<certificates.size();i++){
                KeystoreCertificate keystoreCertificate = new KeystoreCertificate(
                        alias: "IMPORTED_CHANGE_ME" + i,
                        certificate: certificates.get(i),
                )
                if(certificates.get(i).privateKey != null) keystoreCertificate.setKeypair(true)
                keystoreCertificates.add(keystoreCertificate)
            }
        }else {
            keystoreCertificates = certificateLoader.decodeImportPCKS12(certificateImportDto.getBase64File(), certificateImportDto.getPassword())
            //LOG.info("Well fuck seems like the developer hasnt implemented this yet")
        }

        List<Keystore> keystores = new ArrayList<>()
        certificateImportDto.getSelectedKeystores().forEach(r->{
            Keystore keystore = keystoreRepository.findById(r.getId()).get()
            keystores.add(keystore)
        })

        propCertToAgents(keystoreCertificates,keystores)

    }

    void propCertToAgents(List<KeystoreCertificate> certificates, List<Keystore> keystores){

        List<KeystoreCertificateDto> keystoreCertificateDtos = toDto(certificates)

        for(int i=0; i<keystores.size();i++){
            Instance instance = keystores.get(i).getInstance()
            PreparedRequest preparedRequest = new PreparedRequest()
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<List<KeystoreCertificateDto>> request = new HttpEntity<>(keystoreCertificateDtos, preparedRequest.getHeader(instance));
            ResponseEntity<String> response
            try{
                response = restTemplate.postForEntity(instance.getAccessUrl() + api_url + "/addToKeystore/" + keystores.get(i).getAgentId(), request, String.class)
                LOG.info("Propagated keystore certificates to keystore {} on instance {}",keystores.get(i).getLocation(),instance.getName())
            } catch(Exception e){
                LOG.error("Propagation of keystore certificates failed with error: " + e )
            }


        }
    }
    //Refactored
    void delete(List<CertificateFormGUI> certificateFormGUIS){
        List <KeystoreCertificate> certificates = new ArrayList<>()
        certificateFormGUIS.forEach(r->{
            KeystoreCertificate certificate = repository.findById(r.id).get()
            certificates.add(certificate)
        })
        certificates.forEach(r->{
            Keystore keystore = keystoreRepository.findById(r.getKeystoreId()).get()
            Instance instance = keystore.getInstance()
            PreparedRequest preparedRequest = new PreparedRequest()
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity request = new HttpEntity<>(preparedRequest.getHeader(instance))

            ResponseEntity response = restTemplate.exchange(
                    instance.getAccessUrl() + api_url + "/" + r.getKeystoreId(),
                    HttpMethod.DELETE,
                    request,
                    String.class
            );

            /*
            List<Certificate> tmpcerts = keystore.getCertificates()
            tmpcerts.remove(r)
            keystore.setCertificates(tmpcerts)
            keystoreRepository.save(keystore)
            repository.delete(r)
            *
             */
            LOG.info("Deleted certificate with alias {}, subject DN {} in keystore {} on instance {}", r.getAlias(),r.getCertificate().getX509Certificate().getSubjectDN(),keystore.getLocation(),instance.getName())

        })
    }

    //Refactored all below
    KeystoreCertificateDto toDto(KeystoreCertificate keystoreCertificate){
        CertificateLoader certificateLoader = new CertificateLoader()
        KeystoreCertificateDto keystoreCertificateDto = new KeystoreCertificateDto(
                id: keystoreCertificate.id,
                agentId: keystoreCertificate.agentId,
                alias: keystoreCertificate.alias,
                certificateDto: certificateToCertificateDto(keystoreCertificate.certificate),
                keystoreId: keystoreCertificate.keystoreId,
                keypair: keystoreCertificate.keypair
        )
        return keystoreCertificateDto
    }

    List<KeystoreCertificateDto> toDto(List<KeystoreCertificate> certificates){
        List<KeystoreCertificateDto> keystoreCertificateDtos = new ArrayList<>()
        certificates.forEach(r->{
            keystoreCertificateDtos.add(toDto(r))
        })
        return keystoreCertificateDtos
    }

    //Manual method
    void assignSignerCert(Long signerCertificateId, List<CertificateFormGUI>  certificateFormGUIS){
        certificateFormGUIS.forEach(r->{
            Certificate certificate = certificateRepository.findById(r.getId()).get()
            certificate.setSignerCertificateId(signerCertificateId)
            certificate.setManaged(true)
            certificateRepository.save(certificate)
        })
    }

    KeystoreCertificate purgeCertDuplicates(KeystoreCertificate keystoreCertificate){
        Certificate certificate = certificateRepository.findByX509Certificate(keystoreCertificate.getCertificate().getX509Certificate())
        if(certificate == null){
            if(environment.getProperty("controller.auto.assign.ca").toBoolean()){
                keystoreCertificate = findAndAssignCa(keystoreCertificate)
            }
            Long savedId = certificateRepository.save(keystoreCertificate.certificate).getId()
            keystoreCertificate.setCertificate(certificateRepository.findById(savedId).get())
        }else{
            if(keystoreCertificate.certificate.privateKey != null && certificate.privateKey == null){
                certificate.setPrivateKey(keystoreCertificate.certificate.privateKey)
                certificateRepository.save(certificate)
            }
            keystoreCertificate.setCertificate(certificate)
        }
        return keystoreCertificate
    }

    List<KeystoreCertificate> purgeCertDuplicates(List<KeystoreCertificate> keystoreCertificates){
        List<KeystoreCertificate> keystoreCertificatesPurged = new ArrayList<>()
        for(int i=0;i<keystoreCertificates.size();i++){
            keystoreCertificatesPurged.add(purgeCertDuplicates(keystoreCertificates.get(i)))
        }
        return keystoreCertificatesPurged
    }

    KeystoreCertificate findAndAssignCa(KeystoreCertificate keystoreCertificate){
        List<CaCertificate> caCertificates = caCertificateRepository.findAll() as List<CaCertificate>
        caCertificates.forEach(r->{
            if(r.getCertificate().getX509Certificate().getSubjectDN() == keystoreCertificate.getCertificate().getX509Certificate().getIssuerDN()){
                try{
                    keystoreCertificate.getCertificate().getX509Certificate().verify(r.getCertificate().getX509Certificate().getPublicKey(), "BC")
                    keystoreCertificate.getCertificate().setSignerCertificateId(r.getId())
                    return keystoreCertificate
                }catch(Exception ignored){
                }
            }
        })
        return keystoreCertificate
    }

    CertificateDto certificateToCertificateDto(Certificate certificate){
        CertificateLoader certificateLoader = new CertificateLoader()
        CertificateDto certificateDto = new CertificateDto(
                id: certificate.id,
                encodedX509Certificate: certificateLoader.encodeX509(certificate.x509Certificate),
                encodedPrivateKey: certificateLoader.encodeKey(certificate.privateKey)
        )
        return certificateDto
    }

}
