package eu.outerheaven.certmanager.controller.service

import eu.outerheaven.certmanager.controller.dto.CertificateDto
import eu.outerheaven.certmanager.controller.dto.KeystoreDto
import eu.outerheaven.certmanager.controller.entity.Certificate
import eu.outerheaven.certmanager.controller.entity.Instance
import eu.outerheaven.certmanager.controller.entity.Keystore
import eu.outerheaven.certmanager.controller.entity.User
import eu.outerheaven.certmanager.controller.form.InstanceForm
import eu.outerheaven.certmanager.controller.form.KeystoreForm
import eu.outerheaven.certmanager.controller.form.KeystoreFormGUI
import eu.outerheaven.certmanager.controller.repository.CertificateRepository
import eu.outerheaven.certmanager.controller.repository.InstanceRepository
import eu.outerheaven.certmanager.controller.repository.KeystoreRepository
import eu.outerheaven.certmanager.controller.repository.UserRepository
import eu.outerheaven.certmanager.controller.util.CertificateLoader
import eu.outerheaven.certmanager.controller.util.PreparedRequest
import eu.outerheaven.certmanager.controller.util.deserializers.X509CertificateDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate


@Service
class KeystoreService {

    private static final Logger LOG = LoggerFactory.getLogger(KeystoreService.class)

    String api_url="/api/keystore"

    @Autowired
    private final KeystoreRepository repository

    @Autowired
    private final InstanceRepository instanceRepository

    @Autowired
    private final UserRepository userRepository

    @Autowired
    private final CertificateRepository certificateRepository

    @Autowired
    private final MailService mailService

    ArrayList<KeystoreForm> getAll(){
        ArrayList<Keystore> all = repository.findAll() as ArrayList<Keystore>
        ArrayList<KeystoreForm> all_form = toForm(all)
        return all_form
    }

    //For GUI use only
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

    KeystoreForm toForm(Keystore keystore){
        KeystoreForm keystoreForm = new KeystoreForm(
                id: keystore.id,
                agentId: keystore.agentId,
                instanceId: keystore.instanceId,
                location: keystore.location,
                description: keystore.description,
                password: keystore.password,
        )
        return keystoreForm
    }

    ArrayList<KeystoreForm> toForm(ArrayList<Keystore> keystores){
        ArrayList<KeystoreForm> keystoreForms = new ArrayList<>()
        keystores.forEach(r -> keystoreForms.add(toForm(r)))
        return keystoreForms
    }

    Keystore toClass(KeystoreForm keystoreForm){
        Keystore keystore = new Keystore(
                id: keystoreForm.id,
                agentId: keystoreForm.agentId,
                instanceId: keystoreForm.instanceId,
                location: keystoreForm.location,
                description: keystoreForm.description,
                password: keystoreForm.password
        )
    }


    //USE ONLY IF PARSING RESPONSE FROM AGENT, MAPPING OF ID IS DIFFRENT
    //TODO
    Keystore toClass(KeystoreDto keystoreDto, Long keystoreId){
        CertificateLoader certificateLoader = new CertificateLoader()
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

    ArrayList<Keystore> toClass(ArrayList<KeystoreForm> keystoreForms){
        ArrayList<Keystore> keystores = new ArrayList<>()
        keystoreForms.forEach(r ->keystores.add(toClass(r)))
        return keystores
    }


    void add(ArrayList<KeystoreForm> keystoreForms){
        keystoreForms.forEach(r ->{
            Keystore keystore = toClass(r)
            try{
                Long keystoreId = repository.save(keystore).getId()
                LOG.info("Keystore has been added to controller database, pushing now to agent")
                KeystoreDto keystoreDto = addToInstance(r)
                keystore = toClass(keystoreDto, keystoreId)
                repository.save(keystore)
            }catch(Exception exception){
                LOG.error("Failed to add new keystore with error: " + exception)
            }


        })
    }

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
        List<Certificate> certificates = new ArrayList<>()
        List<Certificate> currentcertificates = targetKeystore.getCertificates()
        List<Certificate> unchangedCertificates = new ArrayList<>()
        List<Certificate> modifiedCertificates = new ArrayList<>()
        List<Certificate> addedCertificates = new ArrayList<>()
        List<Certificate> removedCertificates = new ArrayList<>()

        keystore.getCertificates().forEach(r->{
            Certificate certificate = r
            if(currentcertificates != null){
                Certificate tmpCertificate = currentcertificates.stream()
                        .filter(tmp -> certificate.getAgent_id().equals(tmp.getAgent_id()))
                        .findAny()
                        .orElse(null);
                if(tmpCertificate == null){
                    LOG.info("Found new certificate with alias {}", certificate.alias)
                    addedCertificates.add(certificate)
                }else {
                    if (tmpCertificate.x509Certificate == certificate.x509Certificate && tmpCertificate.privateKey == certificate.privateKey) {
                        LOG.info("Found unmodified certificate")
                        unchangedCertificates.add(tmpCertificate)
                        currentcertificates.remove(tmpCertificate)
                    }else{
                        LOG.info("Found modified certificate")
                        certificate.setId(tmpCertificate.getId())
                        modifiedCertificates.add(certificate)
                        currentcertificates.remove(tmpCertificate)
                    }
                }
            }
        })
        if(currentcertificates != null) removedCertificates.addAll(currentcertificates)
        LOG.info("Found {} deleted certificates",removedCertificates.size())
        targetKeystore.getCertificates().clear()
        targetKeystore.getCertificates().addAll(unchangedCertificates)
        targetKeystore.getCertificates().addAll(modifiedCertificates)
        targetKeystore.getCertificates().addAll(addedCertificates)

        /*
        certificates.addAll(unchangedCertificates)
        certificates.addAll(modifiedCertificates)
        certificates.addAll(addedCertificates)
        targetKeystore.setCertificates(certificates)
        */
        repository.save(targetKeystore)
        boolean mailAlert = false
        if(mailAlert){
            mailService.sendKeystoreAlert(modifiedCertificates, addedCertificates, removedCertificates,instance,targetKeystore)
        }
        removedCertificates.forEach(z->{
            LOG.info("Certificate with alias {} and ID {} has been removed",z.alias, z.id)
            //certificateRepository.deleteById(z.getId())
        })
    }

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
                LOG.info("Deleting keystore from database on instance {}, under the path {}", instanceRepository.findById(r.instanceId).get().name, r.location)
                repository.deleteById(r.id)
            }catch(Exception exception){
                LOG.error("Error while trying to delete keystore with ID {}: {}",r.id, exception)
            }
        })
    }

    private void deleteFromAgent(Keystore keystore){

        Instance instance = instanceRepository.findById(keystore.getInstanceId()).get()
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

}
