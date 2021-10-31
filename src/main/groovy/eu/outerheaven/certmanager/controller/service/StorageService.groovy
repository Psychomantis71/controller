package eu.outerheaven.certmanager.controller.service

import eu.outerheaven.certmanager.controller.dto.KeystoreDto
import eu.outerheaven.certmanager.controller.dto.PayloadUploadDto
import eu.outerheaven.certmanager.controller.entity.Instance
import eu.outerheaven.certmanager.controller.form.InstanceForm
import eu.outerheaven.certmanager.controller.form.KeystoreForm
import eu.outerheaven.certmanager.controller.form.PayloadLocationForm
import eu.outerheaven.certmanager.controller.form.PayloadLocationFormGUI
import eu.outerheaven.certmanager.controller.form.PayloadUploadForm
import eu.outerheaven.certmanager.controller.repository.InstanceRepository
import eu.outerheaven.certmanager.controller.util.CertificateLoader
import eu.outerheaven.certmanager.controller.util.PreparedRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartFile

@Service
class StorageService {

    private static final Logger LOG = LoggerFactory.getLogger(KeystoreService.class)

    String api_url="/api/files"

    @Autowired
    private final InstanceRepository instanceRepository

    List<PayloadLocationFormGUI> fetchAllLocations(){
        List<Instance> allInstances = instanceRepository.findAll() as List<Instance>
        List<PayloadLocationFormGUI> payloadLocationFormGUIS = new ArrayList<>()
        Long identId = 0
        for(int i=0; i<allInstances.size(); i++){
            Instance instance = allInstances.get(i)
            PreparedRequest preparedRequest = new PreparedRequest()
            RestTemplate restTemplate = new RestTemplate()
            HttpEntity request = new HttpEntity(preparedRequest.getHeader(instance))

            ResponseEntity<List<PayloadLocationForm>> response = restTemplate.exchange(
                    instance.getAccessUrl() + api_url + "/all-locations",
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<List<PayloadLocationForm>>(){}
            )
            List<PayloadLocationForm> responseForms = response.getBody()
            responseForms.forEach(r->{
                PayloadLocationFormGUI payloadLocationFormGUI = new PayloadLocationFormGUI(
                        id: identId,
                        agentId: r.id,
                        pathName: r.name,
                        path: r.location,
                        instanceId: instance.id,
                        instanceName: instance.name,
                        hostname: instance.hostname
                )
                identId++
                payloadLocationFormGUIS.add(payloadLocationFormGUI)
            })
        }

        return payloadLocationFormGUIS
    }

    void removeLocations(List<PayloadLocationFormGUI> payloadLocationFormGUIS){

        payloadLocationFormGUIS.forEach(r -> {
            Instance instance = instanceRepository.findById(r.instanceId).get()
            PreparedRequest preparedRequest = new PreparedRequest()
            RestTemplate restTemplate = new RestTemplate();
            HttpEntity request = new HttpEntity(preparedRequest.getHeader(instance))
            try{
                ResponseEntity<String> response = restTemplate.exchange(
                        instance.getAccessUrl() + api_url + "/remove-location/" + r.getAgentId(),
                        HttpMethod.DELETE,
                        request,
                        String.class
                )
                LOG.info("Payload location {} removed from instance {}",r.path, instance.name)
            } catch(Exception e){
                LOG.error("Error removing payload location " + e )
            }

        })
    }

    PayloadLocationFormGUI addLocation(PayloadLocationFormGUI payloadLocationFormGUI){
        LOG.info("Info for request is: {} {} {} ", payloadLocationFormGUI.instanceId, payloadLocationFormGUI.pathName, payloadLocationFormGUI.path)
        Instance instance = instanceRepository.findById(payloadLocationFormGUI.instanceId).get()
        PayloadLocationForm payloadLocationForm = new PayloadLocationForm(
                name: payloadLocationFormGUI.pathName,
                location: payloadLocationFormGUI.path
        )
        PreparedRequest preparedRequest = new PreparedRequest()
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<PayloadLocationForm> request = new HttpEntity<>(payloadLocationForm, preparedRequest.getHeader(instance))
        ResponseEntity<String> response
        try{
            response = restTemplate.postForEntity(instance.getAccessUrl() + api_url + "/add-location", request, String.class)
            LOG.info("New payload location {} added to: {}",payloadLocationForm.location,instance.name)
        } catch(Exception e){
            LOG.error("Error while adding payload location " + e )
        }
    }

    void uploadFile(PayloadUploadForm payloadUploadForm){
        LOG.info("Data for file is : {} ", payloadUploadForm.getBase64file())
        List<PayloadLocationFormGUI> payloadLocationFormGUIS = payloadUploadForm.payloadLocationFormGUIS
        payloadLocationFormGUIS.forEach(r -> {
            Instance instance = instanceRepository.findById(r.instanceId).get()
            PreparedRequest preparedRequest = new PreparedRequest()
            RestTemplate restTemplate = new RestTemplate();
            PayloadUploadDto payloadUploadDto = new PayloadUploadDto(
                    name: payloadUploadForm.name,
                    base64file: payloadUploadForm.base64file,
                    payloadLocationId: r.agentId
            )
            HttpEntity<PayloadUploadDto> request = new HttpEntity<>(payloadUploadDto, preparedRequest.getHeader(instance))
            try{
                ResponseEntity<String> result = restTemplate.postForEntity(instance.getAccessUrl() + api_url + "/upload-file", request, String.class)
                LOG.info("File upload of file {} to instance {} response: ", payloadUploadForm.name, instance.name )
            }catch(Exception exception){
                LOG.error("File upload failed with exception: ", exception)
            }
        })
    }


}
