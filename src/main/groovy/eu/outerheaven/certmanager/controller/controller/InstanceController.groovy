package eu.outerheaven.certmanager.controller.controller

import eu.outerheaven.certmanager.controller.form.InstanceForm
import eu.outerheaven.certmanager.controller.service.InstanceService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/instance")
class InstanceController {

    private final Logger log = LoggerFactory.getLogger(InstanceController)

    @Autowired
    private final InstanceService service

    @PostMapping("/request-adoption")
    ResponseEntity adoptRequest(@RequestBody InstanceForm form) {
        if(service.adoptRequest(form)){
            log.info("Entity with IP {}, hostname {} and port {} has been added as pending for adoption!", form.ip,form.hostname,form.port)
            return ResponseEntity.ok().body("Adoption pending from administrator")
            //return new ResponseEntity<String>("Adoption pending from administrator", HttpStatus.OK);

        }else{
            log.info("An entity was rejected for adoption as one with the same IP/hostname and port is already present!")
            return ResponseEntity.badRequest().body("A entity already exists under that IP and port!")
        }
    }

    @GetMapping("/all")
    ResponseEntity all(){
        return ResponseEntity.ok(service.getAll())
    }

    @PutMapping("/adopt")
    ResponseEntity adopt(@RequestBody ArrayList<InstanceForm> form){
        try {
            service.adopt(form)
            return ResponseEntity.ok().body("Entities adopted")
        }catch(Exception exception){
            log.error("Adoption failed with error message: " + exception)
            return ResponseEntity.internalServerError().body(exception)
        }


    }
}




