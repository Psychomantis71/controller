package eu.outerheaven.certmanager.controller.service

import eu.outerheaven.certmanager.controller.entity.Instance
import eu.outerheaven.certmanager.controller.form.PayloadLocationFormGUI
import eu.outerheaven.certmanager.controller.repository.InstanceRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StorageService {

    private static final Logger LOG = LoggerFactory.getLogger(KeystoreService.class)

    String api_url="/api/files"

    @Autowired
    private final InstanceRepository instanceRepository

    List<PayloadLocationFormGUI> fetchAllLocations(){
        List<Instance> allInstances = instanceRepository.findAll() as List<Instance>

        for(int i=0; i<allInstances.size(); i++){
            
        }
    }


}
