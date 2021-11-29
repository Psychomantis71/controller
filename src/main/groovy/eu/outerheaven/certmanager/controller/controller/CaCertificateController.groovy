package eu.outerheaven.certmanager.controller.controller

import eu.outerheaven.certmanager.controller.form.CaCertificateForm
import eu.outerheaven.certmanager.controller.form.NewSignedCertificateForm
import eu.outerheaven.certmanager.controller.service.CaVaultService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
        ResponseEntity.ok(service.getAllCaCertsGUI())
    }

    @PostMapping("/add-root")
    ResponseEntity addRoot(@RequestBody CaCertificateForm caCertificateForm){
        ResponseEntity.ok(service.createRootCert(caCertificateForm))
    }

    @PostMapping("/add-signed")
    ResponseEntity addSigned(@RequestBody NewSignedCertificateForm newSignedCertificateForm){
        ResponseEntity.ok(service.createSignedCert(newSignedCertificateForm))
    }

    @GetMapping("/{certificateId}/export-pem")
    ResponseEntity<Resource> exportAsFile(@PathVariable Long certificateId){

        LOG.info("Export pem controller called")
        String filename = service.getCleanCertName(certificateId)
        Resource resource = service.exportAsPem(certificateId, filename)

        try{
            LOG.info("Filename on response is:" + filename)
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"").body(resource)
        }catch(Exception exception){
            LOG.error("Error while exporting file: " + exception)
        }

    }

    @PostMapping("/import")
    ResponseEntity importCert(@RequestBody NewSignedCertificateForm newSignedCertificateForm){
        ResponseEntity.ok(service.createSignedCert(newSignedCertificateForm))
    }

    @PostMapping("/replace")
    ResponseEntity replace(@RequestBody NewSignedCertificateForm newSignedCertificateForm){
        ResponseEntity.ok(service.createSignedCert(newSignedCertificateForm))
    }

}
