package eu.outerheaven.certmanager.controller.controller

import eu.outerheaven.certmanager.controller.form.CaCertificateForm
import eu.outerheaven.certmanager.controller.service.CaVaultService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/cavault")
class CaCertificateController {

    private final Logger LOG = LoggerFactory.getLogger(CaCertificateController)

    @Autowired
    private final CaVaultService service

    @GetMapping("/all-gui")
    ResponseEntity allGUI(){
        return ResponseEntity.ok(service.getAllGUI())
    }

    @PostMapping("/add-root")
    ResponseEntity addRoot(@RequestBody CaCertificateForm caCertificateForm){
        return ResponseEntity.ok(service.createRootCert(caCertificateForm))
    }

}
