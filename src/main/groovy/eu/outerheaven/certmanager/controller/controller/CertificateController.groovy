package eu.outerheaven.certmanager.controller.controller

import eu.outerheaven.certmanager.controller.form.KeystoreForm
import eu.outerheaven.certmanager.controller.service.CertificateService
import eu.outerheaven.certmanager.controller.service.KeystoreService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/certificate")
class CertificateController {

    private final Logger LOG = LoggerFactory.getLogger(CertificateController)

    @Autowired
    private final CertificateService service

    @GetMapping("/all-gui")
    ResponseEntity allGUI(){
        return ResponseEntity.ok(service.getAllGUI())
    }

    @PostMapping("/add")
    ResponseEntity add(@RequestBody ArrayList<KeystoreForm> keystoreForms){
        return ResponseEntity.ok("OK")
    }

    @PutMapping("/update")
    ResponseEntity update(@RequestBody KeystoreForm keystoreForm){
        return ResponseEntity.ok("OK")
    }

    @DeleteMapping("/delete")
    ResponseEntity delete(@RequestBody KeystoreForm keystoreForm){
        return ResponseEntity.ok("OK")
    }

}
