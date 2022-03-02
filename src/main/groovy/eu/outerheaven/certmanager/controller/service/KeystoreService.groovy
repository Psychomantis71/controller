package eu.outerheaven.certmanager.controller.service

import eu.outerheaven.certmanager.controller.dto.CertificateDto
import eu.outerheaven.certmanager.controller.dto.KeystoreCertificateDto
import eu.outerheaven.certmanager.controller.dto.KeystoreDto
import eu.outerheaven.certmanager.controller.entity.CaCertificate
import eu.outerheaven.certmanager.controller.entity.Certificate
import eu.outerheaven.certmanager.controller.entity.Instance
import eu.outerheaven.certmanager.controller.entity.Keystore
import eu.outerheaven.certmanager.controller.entity.KeystoreCertificate
import eu.outerheaven.certmanager.controller.entity.User
import eu.outerheaven.certmanager.controller.form.KeystoreForm
import eu.outerheaven.certmanager.controller.form.KeystoreFormGUI
import eu.outerheaven.certmanager.controller.form.RetrieveFromPortForm
import eu.outerheaven.certmanager.controller.repository.CertificateRepository
import eu.outerheaven.certmanager.controller.repository.InstanceRepository
import eu.outerheaven.certmanager.controller.repository.KeystoreCertificateRepository
import eu.outerheaven.certmanager.controller.repository.KeystoreRepository
import eu.outerheaven.certmanager.controller.repository.UserRepository
import eu.outerheaven.certmanager.controller.util.CertificateLoader
import eu.outerheaven.certmanager.controller.util.PreparedRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.env.Environment
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate

import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException



@Service
@Component
class KeystoreService {

    private static final Logger LOG = LoggerFactory.getLogger(KeystoreService.class)

    String api_url="/api/keystore"

    @Autowired
    private final KeystoreRepository repository

    @Autowired
    private final KeystoreCertificateRepository keystoreCertificateRepository

    @Autowired
    private final InstanceRepository instanceRepository

    @Autowired
    private final UserRepository userRepository

    @Autowired
    private final CertificateRepository certificateRepository

    @Autowired
    private final MailService mailService

    @Autowired
    private final CaVaultService caVaultService

    @Autowired
    private Environment environment

    @Autowired
    private final KeystoreCertificateService keystoreCertificateService

    //Refactored
    ArrayList<KeystoreForm> getAll(){
        ArrayList<Keystore> all = repository.findAll() as ArrayList<Keystore>
        ArrayList<KeystoreForm> all_form = toForm(all)
        return all_form
    }

    //For GUI use only
    //Refactored
    ArrayList<KeystoreFormGUI> getAllGUI(){
        ArrayList<KeystoreForm> keystoreForms = getAll()
        ArrayList<KeystoreFormGUI> keystoreFormGUIS = new ArrayList<>()
        for(int i=0;i<keystoreForms.size();i++){
            Instance instance = instanceRepository.findById(keystoreForms.get(i).getInstanceId()).get()
            KeystoreFormGUI keystoreFormGUI = new KeystoreFormGUI(
                    id: keystoreForms.get(i).id,
                    description: keystoreForms.get(i).description,
                    location: keystoreForms.get(i).location,
                    instanceName: instance.name,
                    hostname: instance.hostname

            )
            keystoreFormGUIS.add(keystoreFormGUI)
        }
        return keystoreFormGUIS
    }
    //Refactored
    KeystoreForm toForm(Keystore keystore){
        KeystoreForm keystoreForm = new KeystoreForm(
                id: keystore.id,
                agentId: keystore.agentId,
                instanceId: keystore.instance.id,
                location: keystore.location,
                description: keystore.description,
                password: keystore.password,
        )
        return keystoreForm
    }
    //Refactored
    ArrayList<KeystoreForm> toForm(ArrayList<Keystore> keystores){
        ArrayList<KeystoreForm> keystoreForms = new ArrayList<>()
        keystores.forEach(r -> keystoreForms.add(toForm(r)))
        return keystoreForms
    }
    //Refactored
    Keystore toClass(KeystoreForm keystoreForm){
        Keystore keystore = new Keystore(
                id: keystoreForm.id,
                agentId: keystoreForm.agentId,
                instance: instanceRepository.findById(keystoreForm.instanceId).get(),
                location: keystoreForm.location,
                description: keystoreForm.description,
                password: keystoreForm.password
        )
    }

    //USE ONLY IF PARSING RESPONSE FROM AGENT, MAPPING OF ID IS DIFFRENT
    //TODO
    /*
    OLD
    Keystore toClass(KeystoreDto keystoreDto, Long keystoreId){
        CertificateLoader certificateLoader = new CertificateLoader()
        List<KeystoreCertificateDto> keystoreCertificateDtos
        List<Certificate> certificates = new ArrayList<>()
        keystoreDto.getCertificates().forEach(r->{
            Certificate certificate = new Certificate(
                    agent_id: r.id,
                    alias: r.alias,
                    privateKey: certificateLoader.decodeKey(r.key),
                    x509Certificate: certificateLoader.decodeX509(r.encodedX509),
                    managed: r.managed,
                    keystoreId: keystoreId
            )
            certificates.add(certificate)
        })
        Keystore keystore = new Keystore(
                id: keystoreId,
                agentId: keystoreDto.id,
                instanceId: repository.findById(keystoreId).get().getInstanceId(),
                location: keystoreDto.location,
                description: keystoreDto.description,
                password: keystoreDto.password,
                certificates: certificates
        )
        return  keystore
    }
    */

    //Refactored
    Keystore toClass(KeystoreDto keystoreDto, Long keystoreId){
        CertificateLoader certificateLoader = new CertificateLoader()
        List<KeystoreCertificate> keystoreCertificates = new ArrayList<>()

        keystoreDto.getKeystoreCertificateDtos().forEach(r->{
            Certificate certificate = new Certificate(
                    x509Certificate: certificateLoader.decodeX509(r.certificateDto.encodedX509Certificate),
                    privateKey: certificateLoader.decodeKey(r.certificateDto.encodedPrivateKey),

            )
            KeystoreCertificate keystoreCertificate = new KeystoreCertificate(
                    agentId: r.id,
                    alias: r.alias,
                    certificate: certificate,
                    keystoreId: keystoreId,
                    keypair: r.keypair
            )
            keystoreCertificates.add(keystoreCertificate)
        })
        Keystore keystore = new Keystore(
                id: keystoreId,
                agentId: keystoreDto.id,
                instance: repository.findById(keystoreId).get().getInstance(),
                location: keystoreDto.location,
                description: keystoreDto.description,
                password: keystoreDto.password,
                keystoreCertificates: keystoreCertificates
        )
        return  keystore
    }

    //Refactored
    ArrayList<Keystore> toClass(ArrayList<KeystoreForm> keystoreForms){
        ArrayList<Keystore> keystores = new ArrayList<>()
        keystoreForms.forEach(r ->keystores.add(toClass(r)))
        return keystores
    }

    //Refactored
    void add(ArrayList<KeystoreForm> keystoreForms){
        keystoreForms.forEach(r ->{
            Keystore keystore = toClass(r)
            try{
                Long keystoreId = repository.save(keystore).getId()
                LOG.info("Keystore has been added to controller database, pushing now to agent")
                KeystoreDto keystoreDto = addToInstance(r)
                keystore = toClass(keystoreDto, keystoreId)
                keystore.setKeystoreCertificates(keystoreCertificateService.purgeCertDuplicates(keystore.keystoreCertificates))
                repository.save(keystore)
            }catch(Exception exception){
                LOG.error("Failed to add new keystore with error: " + exception)
            }


        })
    }

    //Refactored
    private KeystoreDto addToInstance(KeystoreForm keystoreForm){
        Instance instance = instanceRepository.findById(keystoreForm.getInstanceId()).get()
        PreparedRequest preparedRequest = new PreparedRequest()
        CertificateLoader certificateLoader = new CertificateLoader()
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<KeystoreForm> request = new HttpEntity<>(keystoreForm, preparedRequest.getHeader(instance))
        ResponseEntity<KeystoreDto> response
        try{
            response = restTemplate.postForEntity(instance.getAccessUrl() + api_url + "/add", request, KeystoreDto.class)
            LOG.info("Agent with ip {}, hostname {} and port {} has added a new keystore!",instance.getIp(),instance.getHostname(),instance.getPort())
            return response.getBody()
        } catch(Exception e){
            LOG.error("Adding keystore to agent failed with error " + e )
        }

    }

    void updateKeystore(KeystoreDto keystoreDto, String username, String ip){
        User user = userRepository.findByUserName(username)
        LOG.info("User {} is requesting a to update a keystore", user.getUserName())
        Instance instance = instanceRepository.findByUser(user)
        LOG.info("The assigned instance/agent to user {} is {}", user.getUserName(), instance.getName())
        if(instance.getIp() == ip){
            LOG.info("User is accesing the endpoint from the expected IP address {}",ip)
        }else{
            LOG.warn("User is accesing the endpoint from a IP that does not match the agent IP {}",ip)
        }
        if(!instance.getAdopted()) throw new Exception("Off to the orphanage with yo ass")
        Keystore targetKeystore = repository.findByInstanceIdAndAgentId(instance.getId(), keystoreDto.getId())
        Keystore keystore = toClass(keystoreDto, targetKeystore.getId())
        //TODO diffrence comparrison
        List<KeystoreCertificate> certificates = new ArrayList<>()
        List<KeystoreCertificate> currentcertificates = targetKeystore.getKeystoreCertificates()
        List<KeystoreCertificate> unchangedCertificates = new ArrayList<>()
        List<KeystoreCertificate> modifiedCertificates = new ArrayList<>()
        List<KeystoreCertificate> addedCertificates = new ArrayList<>()
        List<KeystoreCertificate> removedCertificates = new ArrayList<>()

        keystore.getKeystoreCertificates().forEach(r->{
            LOG.info("Handling now agent kc with data: alias {}, agent ID {}, and certificate DN {}",r.alias,r.agentId,r.certificate.x509Certificate.subjectDN)
            KeystoreCertificate certificate = r
            if(currentcertificates != null){
                KeystoreCertificate tmpCertificate = currentcertificates.stream()
                        .filter(tmp -> certificate.getAgentId().equals(tmp.getAgentId()))
                        .findAny()
                        .orElse(null);
                if(tmpCertificate == null){
                    LOG.info("Found new certificate with alias {}", certificate.alias)
                    addedCertificates.add(certificate)
                }else {
                    if (tmpCertificate.certificate.x509Certificate == certificate.certificate.x509Certificate && tmpCertificate.certificate.privateKey == certificate.certificate.privateKey) {
                        LOG.info("Found unmodified certificate, agent ID {}, local ID {}",tmpCertificate.agentId,tmpCertificate.id)
                        unchangedCertificates.add(tmpCertificate)
                        currentcertificates.remove(tmpCertificate)
                    }else{
                        LOG.info("Found modified certificate, agent ID {}, local ID {}",tmpCertificate.agentId,tmpCertificate.id)
                        LOG.info("Existing entry is as follows: ID {}, agent ID {}, alias {}, certificate DN {}, certificate not before {}, certificate not after {}",tmpCertificate.id, tmpCertificate.agentId,tmpCertificate.alias,tmpCertificate.certificate.x509Certificate.subjectDN,tmpCertificate.certificate.x509Certificate.notBefore,tmpCertificate.certificate.x509Certificate.notAfter)
                        LOG.info("New entry is as follows:, agent ID {}, alias {}, certificate DN {}, certificate not before {}, certificate not after {}",certificate.agentId,certificate.alias,certificate.certificate.x509Certificate.subjectDN,certificate.certificate.x509Certificate.notBefore,certificate.certificate.x509Certificate.notAfter)
                        //certificate.setId(tmpCertificate.id)
                        tmpCertificate.setCertificate(certificate.certificate)
                        tmpCertificate.setAlias(certificate.alias)
                        tmpCertificate.setKeypair(certificate.keypair)
                        LOG.info("Adjusted modified certificate data is now, agent ID {}, local ID {}",tmpCertificate.agentId,tmpCertificate.id)
                        modifiedCertificates.add(tmpCertificate)
                        currentcertificates.remove(tmpCertificate)
                    }
                }
            }
        })
        if(currentcertificates != null) removedCertificates.addAll(currentcertificates)
        LOG.info("Found {} deleted certificates",removedCertificates.size())
        targetKeystore.getKeystoreCertificates().clear()
        targetKeystore.getKeystoreCertificates().addAll(unchangedCertificates)
        modifiedCertificates = keystoreCertificateService.purgeCertDuplicates(modifiedCertificates)
        LOG.info("Modified contents after purging duplicates: ")
        modifiedCertificates.forEach(r->{
            LOG.info("Keystore certificate ID {}, keystore certificate agent ID {}, Certificate ID {}, Certificate DN {}",r.id,r.agentId,r.certificate.id,r.certificate.x509Certificate.subjectDN)
        })
        targetKeystore.getKeystoreCertificates().addAll(modifiedCertificates)
        addedCertificates = keystoreCertificateService.purgeCertDuplicates(addedCertificates)
        targetKeystore.getKeystoreCertificates().addAll(addedCertificates)

        /*
        certificates.addAll(unchangedCertificates)
        certificates.addAll(modifiedCertificates)
        certificates.addAll(addedCertificates)
        targetKeystore.setCertificates(certificates)
        */
        repository.save(targetKeystore)

        Keystore check = repository.findById(targetKeystore.id).get()

        LOG.info("Keystore contents after saving: ")
        check.keystoreCertificates.forEach(r->{
            LOG.info("Keystore certificate ID {}, keystore certificate agent ID {}, Certificate ID {}, Certificate DN {}",r.id,r.agentId,r.certificate.id,r.certificate.x509Certificate.subjectDN)
        })

        boolean mailAlert = false
        if (environment.getProperty("controller.mail.alert").toBoolean() && environment.getProperty("controller.mail.modification.alert").toBoolean()){
            mailAlert= true
        }
        if(mailAlert){
            mailService.sendKeystoreAlert(modifiedCertificates, addedCertificates, removedCertificates,instance,targetKeystore)
        }
        removedCertificates.forEach(z->{
            LOG.info("Certificate with alias {} and ID {} has been removed",z.alias, z.id)
            //certificateRepository.deleteById(z.getId())
        })
    }
    //Refactored
    void delete(List<KeystoreFormGUI> keystoreFormGUIS){
        List<Keystore> keystores = new ArrayList<>()
        keystoreFormGUIS.forEach(r->{
            Keystore keystore = repository.findById(r.id).get()
            keystores.add(keystore)
        })

        keystores.forEach(r->{
            try{
                LOG.info("Deleting keystore from agent")
                deleteFromAgent(r)
                LOG.info("Deleting keystore from database on instance {}, under the path {}", instanceRepository.findById(r.instance.id).get().name, r.location)
                repository.deleteById(r.id)
            }catch(Exception exception){
                LOG.error("Error while trying to delete keystore with ID {}: {}",r.id, exception)
            }
        })
    }
    //Refactored
    private void deleteFromAgent(Keystore keystore){

        Instance instance = instanceRepository.findById(keystore.instance.id).get()
        PreparedRequest preparedRequest = new PreparedRequest()
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity request = new HttpEntity<>(preparedRequest.getHeader(instance))

        ResponseEntity response = restTemplate.exchange(
                instance.getAccessUrl() + api_url + "/" + keystore.getAgentId(),
                HttpMethod.DELETE,
                request,
                String.class
        );
        LOG.debug("Delete from agent returns: " + response.statusCode)

    }

    //TODO feature
    void retrieveFromAgent(RetrieveFromPortForm retrieveFromPortForm){
        try{
            Instance instance = instanceRepository.findById(retrieveFromPortForm.instanceId).get()
            PreparedRequest preparedRequest = new PreparedRequest()
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<RetrieveFromPortForm> request = new HttpEntity<>(retrieveFromPortForm, preparedRequest.getHeader(instance))

            ResponseEntity<List<CertificateDto>> response = restTemplate.exchange(
                    instance.getAccessUrl() + api_url + "/retrieve-port",
                    HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<List<CertificateDto>>(){}
            )
            List<CertificateDto> responseForms = response.getBody()
            LOG.info("Agent with ip {}, hostname {} and port {} has added a new keystore!",instance.getIp(),instance.getHostname(),instance.getPort())
        } catch(Exception e){
            LOG.error("Adding keystore to agent failed with error " + e )
        }

    }




}
