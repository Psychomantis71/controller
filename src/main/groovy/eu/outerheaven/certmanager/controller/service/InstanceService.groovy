package eu.outerheaven.certmanager.controller.service

import eu.outerheaven.certmanager.controller.entity.Instance
import eu.outerheaven.certmanager.controller.entity.InstanceAccessData
import eu.outerheaven.certmanager.controller.entity.Keystore
import eu.outerheaven.certmanager.controller.entity.User
import eu.outerheaven.certmanager.controller.entity.UserRole
import eu.outerheaven.certmanager.controller.form.InstanceForm
import eu.outerheaven.certmanager.controller.form.UserForm
import eu.outerheaven.certmanager.controller.repository.InstanceRepository
import eu.outerheaven.certmanager.controller.repository.KeystoreRepository
import eu.outerheaven.certmanager.controller.repository.UserRepository
import eu.outerheaven.certmanager.controller.util.CertificateLoader
import eu.outerheaven.certmanager.controller.util.PreparedRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.client.RestTemplate

@Service
class InstanceService {

    String api_url = "/api/instance"

    private static final Logger LOG = LoggerFactory.getLogger(InstanceService.class)

    @Value('${agent.defaultPassword}')
    private String defaultPassword

    @Autowired
    private final InstanceRepository repository

    @Autowired
    private final UserRepository userRepository

    @Autowired
    private final KeystoreRepository keystoreRepository

    boolean adoptRequest(InstanceForm form, String ip){
        if(form.getIp() != ip){
            LOG.warn("The self reported IP from the agent does not match up with the IP the request arrived from, will use the request IP")
            form.setIp(ip)
        }
        Instance instance = new Instance(
                name: form.name,
                hostname: form.hostname,
                ip: form.ip,
                port: form.port,
                adopted: false
        )
        InstanceAccessData tmp = new InstanceAccessData(
                instance: instance,
                password: defaultPassword.toString()
        )

        instance.setInstanceAccessData(tmp)
        if(repository.findByPortAndHostname(instance.getPort(), instance.getHostname()) == null && repository.findByPortAndIp(instance.getPort(), instance.getIp()) == null){

            CertificateLoader  certificateLoader = new CertificateLoader()
            //Generate tmp username so entity can be saved and fetch id
            String tmpusername = certificateLoader.generateRandomName()
            while (userRepository.findByUserName(tmpusername) != null){
                tmpusername = certificateLoader.generateRandomName()
            }
            //generate password for new user
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder()
            String password = certificateLoader.generateRandomAlphanumeric()
            Long newAgentId = createUnadoptedAgent(tmpusername, passwordEncoder.encode(password))
            User newAgent = userRepository.findById(newAgentId).get()
            instance.setUser(newAgent)
            repository.save(instance)

            InstanceForm instanceForm = toForm(instance)
            instanceForm.setNewUsername(newAgent.userName)
            instanceForm.setNewPassword(password)
            PreparedRequest preparedRequest = new PreparedRequest()
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<InstanceForm> request = new HttpEntity<>(instanceForm, preparedRequest.getHeader(instance));
            ResponseEntity<String> response
            try{
                response = restTemplate.postForEntity(instance.getAccessUrl() + api_url + "/update", request, String.class)
                instance.instanceAccessData.setPassword(response.getBody().toString())
                repository.save(instance)
                LOG.info("Entity with IP {}, hostname {}, and port {} has sent an adoption request, assigned user {}!",instance.getIp(),instance.getHostname(),instance.getPort(), instance.getUser().getUserName().toString())
            } catch(Exception e){
                LOG.error("Adoption request encountered an error: " + e )
                LOG.info("Deleting temp user for agent")
                userRepository.deleteById(newAgentId)
            }





            repository.save(instance)
            return true
        }else{
            return false
        }

    }

    ArrayList<InstanceForm> getAll(){
        ArrayList<Instance> all = repository.findAll() as ArrayList<Instance>
        ArrayList<InstanceForm> all_form = toForm(all)
        return all_form

    }

    void adopt(ArrayList<InstanceForm> form){

        for(int i=0;i<form.size();i++){
            Instance instance = repository.findById(form.get(i).getId()).get()
            User user = instance.getUser()
            user.setUserRole(UserRole.AGENT_ADOPTED)
            userRepository.save(user)
            Long userId = userRepository.findByUserName(user.getUserName()).getId()
            user.setUserName("adopted_agent_" + userId)
            LOG.info("Saving new agent user to repository after username change")
            userRepository.save(user)
            instance.setAdopted(true)
            PreparedRequest preparedRequest = new PreparedRequest()
            RestTemplate restTemplate = new RestTemplate();
            form.get(i).setAdopted(true)
            form.get(i).setNewUsername("adopted_agent_" + userId)
            HttpEntity<InstanceForm> request = new HttpEntity<>(form.get(i), preparedRequest.getHeader(instance));
            ResponseEntity<String> response
            try{
                response = restTemplate.postForEntity(instance.getAccessUrl() + api_url + "/update", request, String.class)
                instance.instanceAccessData.setPassword(response.getBody().toString())
                instance.setUser(userRepository.findById(userId).get())
                //MAKNI MEEEEEEEEEEEEEEEEEEEEEEEEEEEEE
                User moj_korisnik = userRepository.findByUserName("admin")
                List<User> korsnici = new ArrayList<>()
                korsnici.add(moj_korisnik)
                instance.setAssignedUsers(korsnici)
                LOG.info("Saving instance data with new user to repository after agent response is positive")
                repository.save(instance)
                LOG.info("Entity with IP {}, hostname {}, and port {} has been adopted and recognized by the target agent!",instance.getIp(),instance.getHostname(),instance.getPort())
            } catch(Exception e){
                LOG.error("Agent adoption failed with error: " + e )
                LOG.info("Deleting temp user for agent")
                userRepository.deleteById(userId)
            }

        }

    }

    List<UserForm> getAssignedUsers(Long instanceId){
        Instance instance = repository.findById(instanceId).get()
        List<UserForm> assignedUsers = toUserForm(instance.getAssignedUsers())
        return assignedUsers
    }

    void setAssignedUsers(Long instanceId, List<Long> userIds){



        Instance instance = repository.findById(instanceId).get()

        LOG.info("Request to assign users to an instance has arrived, a total of {} users will be assigned to instance {}", userIds.size(), instance.getName())

        List<User> users = new ArrayList<>()

        userIds.forEach(r->{
            User user = userRepository.findById(r).get()
            users.add(user)
        })

        instance.setAssignedUsers(users)
        repository.save(instance)
    }

    void removeInstances(List<InstanceForm> instanceForms){

        List<Instance> instances = new ArrayList<>()
        instanceForms.forEach(r-> {
            Instance instance = repository.findById(r.getId()).get()
            instances.add(instance)
        })
        List<Keystore> keystores = new ArrayList<>()
        instances.forEach(r->{
            keystores.addAll(keystoreRepository.findByInstanceId(r.getId()))
        })

        keystores.forEach(r->{
            keystoreRepository.deleteById(r.id)
        })

        instances.forEach(r->{
            repository.deleteById(r.id)
            LOG.info("Deleted instace {} from database, and all keystores/certificates under it", r.getName())
        })


    }

    String generatePassword

    Long createUnadoptedAgent(String tmpusername, String encdedPassword){
        User user = new User()
        user.setUserName(tmpusername)
        user.setPassword(encdedPassword)
        user.setUserRole(UserRole.AGENT)
        userRepository.save(user)
        Long userId = userRepository.findByUserName(tmpusername).getId()
        user.setUserName("unadopted_agent_" + userId)
        userRepository.save(user)
        return userId
    }

    UserForm toUserForm(User user){
        UserForm userForm = new UserForm(
                id: user.id,
                username: user.userName,
                userRole: user.userRole,
                email: user.email
        )
        return userForm
    }

    List<UserForm> toUserForm(List<User> users){
        List<UserForm> userForms = new ArrayList<>()
        users.forEach(r->userForms.add(toUserForm(r)))
        return userForms
    }

    InstanceForm toForm(Instance instance){
        InstanceForm instanceForm = new InstanceForm(
                id: instance.id,
                name: instance.name,
                hostname: instance.hostname,
                ip: instance.ip,
                port: instance.port,
                adopted: instance.adopted
        )
        return instanceForm
    }

    ArrayList<InstanceForm> toForm(ArrayList<Instance> instance){
        ArrayList<InstanceForm> instanceFormArrayList = new ArrayList<>()
        instance.forEach( r -> instanceFormArrayList.add(toForm(r)))
        return instanceFormArrayList
    }

    Instance toClass(InstanceForm form){
        Instance instance = new Instance(
                id: form.id,
                name: form.name,
                hostname: form.hostname,
                ip: form.ip,
                port: form.port,
                adopted: form.adopted
        )
        return instance
    }

    ArrayList<Instance> toClass(ArrayList<InstanceForm> form){
        ArrayList<Instance> instances = new ArrayList<>()
        form.forEach(r ->
                instances.add(toClass(r))
        )
        return instances
    }
}
