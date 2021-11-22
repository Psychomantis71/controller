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

    boolean adoptRequest(InstanceForm form){
        //TODO PULL DEFAULT AGENT PASSWORD WHEN SET IN CONFIG
        User user = userRepository.findByUserName("agent_user")
        Instance instance = new Instance(
                name: form.name,
                hostname: form.hostname,
                ip: form.ip,
                port: form.port,
                adopted: false
        )
        instance.setUser(user)
        InstanceAccessData tmp = new InstanceAccessData(
                instance: instance,
                password: defaultPassword.toString()
        )

        instance.setInstanceAccessData(tmp)
        if(repository.findByPortAndHostname(instance.getPort(), instance.getHostname()) == null && repository.findByPortAndIp(instance.getPort(), instance.getIp()) == null){
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
            CertificateLoader  certificateLoader = new CertificateLoader()
            //Generate tmp username so entity can be saved and fetch id
            String tmpusername = certificateLoader.generateRandomName()
            while (userRepository.findByUserName(tmpusername) != null){
                tmpusername = certificateLoader.generateRandomName()
            }
            //generate password for new user, create user and apply correct username
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder()
            String password = certificateLoader.generateRandomAlphanumeric()
            User user = new User()
            user.setUserName(tmpusername)
            user.setPassword(passwordEncoder.encode(password))
            user.setUserRole(UserRole.AGENT_ADOPTED)
            userRepository.save(user)
            Long userId = userRepository.findByUserName(tmpusername).getId()
            user.setUserName("adopted_agent_" + userId)
            userRepository.save(user)
            instance.setAdopted(true)
            PreparedRequest preparedRequest = new PreparedRequest()
            RestTemplate restTemplate = new RestTemplate();
            form.get(i).setAdopted(true)
            form.get(i).setNewUsername("adopted_agent_" + userId)
            form.get(i).setNewPassword(password)
            HttpEntity<InstanceForm> request = new HttpEntity<>(form.get(i), preparedRequest.getHeader(instance));
            ResponseEntity<String> response
            try{
                response = restTemplate.postForEntity(instance.getAccessUrl() + api_url + "/update", request, String.class)
                instance.instanceAccessData.setPassword(response.getBody().toString())
                instance.setUser(user)
                //MAKNI MEEEEEEEEEEEEEEEEEEEEEEEEEEEEE
                User moj_korisnik = userRepository.findByUserName("admin")
                List<User> korsnici = new ArrayList<>()
                korsnici.add(moj_korisnik)
                instance.setAssignedUsers(korsnici)
                repository.save(instance)
                LOG.info("Entity with IP {}, hostname {}, and port {} has been adopted and recognized by the target agent!",instance.getIp(),instance.getHostname(),instance.getPort())
            } catch(Exception e){
                LOG.error("Agent adoption failed with error: " + e )
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
