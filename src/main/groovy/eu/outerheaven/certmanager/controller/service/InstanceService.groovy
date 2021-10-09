package eu.outerheaven.certmanager.controller.service

import eu.outerheaven.certmanager.controller.entity.Instance
import eu.outerheaven.certmanager.controller.entity.InstanceAccessData
import eu.outerheaven.certmanager.controller.entity.User
import eu.outerheaven.certmanager.controller.entity.UserRole
import eu.outerheaven.certmanager.controller.form.InstanceForm
import eu.outerheaven.certmanager.controller.repository.InstanceRepository
import eu.outerheaven.certmanager.controller.util.PreparedRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.client.RestTemplate

@Service
class InstanceService {

    String api_url = "/api/instance"

    private static final Logger LOG = LoggerFactory.getLogger(InstanceService.class)

    @Autowired
    private final InstanceRepository repository

    boolean adoptRequest(InstanceForm form){
        //TODO PULL DEFAULT AGENT PASSWORD WHEN SET IN CONFIG
        User user = new User(
                userName: "agent_user",
                password: "password",
                userRole: UserRole.AGENT
        )
        Instance instance = new Instance(
                name: form.name,
                hostname: form.hostname,
                ip: form.ip,
                port: form.port,
                adopted: false
        )
        instance.setUser(user)
        instance.setInstanceAccessData(new InstanceAccessData(instance: instance))
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

    void adopt(ArrayList<InstanceForm> form){

        for(int i=0;i<form.size();i++){
            Instance instance = repository.findById(form.get(i).getId()).get()
            instance.setAdopted(true)
            PreparedRequest preparedRequest = new PreparedRequest()
            RestTemplate restTemplate = new RestTemplate();
            form.get(i).setAdopted(true)
            HttpEntity<InstanceForm> request = new HttpEntity<>(form.get(i), preparedRequest.getHeader(instance));
            ResponseEntity<String> response
            try{
                response = restTemplate.postForEntity(instance.getAccessUrl() + api_url + "/update", request, String.class)
                LOG.info(response.getBody().toString())
                repository.save(instance)
                LOG.info("Entity with IP {}, hostname {}, and port {} has been adopted and recognized by the target agent!",instance.getIp(),instance.getHostname(),instance.getPort())
            } catch(Exception e){
                LOG.error("Agent adoption failed with error: " + e )
            }

        }

    }

}
