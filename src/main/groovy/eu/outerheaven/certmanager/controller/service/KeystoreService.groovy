package eu.outerheaven.certmanager.controller.service

import eu.outerheaven.certmanager.controller.dto.CertificateDto
import eu.outerheaven.certmanager.controller.dto.KeystoreDto
import eu.outerheaven.certmanager.controller.entity.Certificate
import eu.outerheaven.certmanager.controller.entity.Instance
import eu.outerheaven.certmanager.controller.entity.Keystore
import eu.outerheaven.certmanager.controller.form.InstanceForm
import eu.outerheaven.certmanager.controller.form.KeystoreForm
import eu.outerheaven.certmanager.controller.form.KeystoreFormGUI
import eu.outerheaven.certmanager.controller.repository.InstanceRepository
import eu.outerheaven.certmanager.controller.repository.KeystoreRepository
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


    //OLD
    void add_old(ArrayList<KeystoreForm> keystoreForms){
        ArrayList<Keystore> responseKeystores = new ArrayList<>()
        keystoreForms.forEach(r->{
            responseKeystores.add(addToInstance(r))
        })
        try {
            responseKeystores.forEach(r -> {
                repository.save(r)
                LOG.debug("Keystore has been added to controller database")
            } )
        }catch(Exception exception){
            LOG.error("Failed to add new keystore with error: " + exception)
        }
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

    //OLD
    private Keystore addToInstance_old(KeystoreForm keystoreForm){
        Instance instance = instanceRepository.findById(keystoreForm.getInstanceId()).get()
        PreparedRequest preparedRequest = new PreparedRequest()
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<KeystoreForm> request = new HttpEntity<>(keystoreForm, preparedRequest.getHeader(instance))
        ResponseEntity<Keystore> response
        try{

            response = restTemplate.exchange(
                    instance.getAccessUrl() + api_url + "/add",
                    HttpMethod.POST,
                    request,
                    Keystore.class
            )

            //response = restTemplate.postForEntity(instance.getAccessUrl() + api_url + "/add", request, Keystore.class)
            LOG.info("Kurac na biciklu")
            Keystore updatedKeystore = new Keystore(
                    agentId: response.getBody().id,
                    instanceId: instance.id,
                    location: response.getBody().location,
                    description: response.getBody().description,
                    password: response.getBody().password,
                    certificates: response.getBody().certificates
            )
            LOG.info("Agent with ip {}, hostname {} and port {} has added a new keystore under the path {}!",instance.getIp(),instance.getHostname(),instance.getPort(), updatedKeystore.getLocation())
            return updatedKeystore
        } catch(Exception e){
            LOG.error("Adding keystore to agent failed with error " + e )
        }

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


}
