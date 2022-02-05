package eu.outerheaven.certmanager.controller.controller

import eu.outerheaven.certmanager.controller.service.CertificateService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/certificate")
class CertificateController {

    @Autowired
    private final CertificateService service

    @GetMapping("/all-gui")
    ResponseEntity allGUI(){
        return ResponseEntity.ok(service.getAllGUI())
    }

}
