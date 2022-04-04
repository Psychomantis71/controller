package eu.outerheaven.certmanager.controller.controller

import eu.outerheaven.certmanager.controller.entity.Certificate
import eu.outerheaven.certmanager.controller.form.RetrieveFromPortForm
import eu.outerheaven.certmanager.controller.service.CertificateService
import eu.outerheaven.certmanager.controller.util.CertificateLoader
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
@RequestMapping("/api/certificate")
class CertificateController {

    @Autowired
    private final CertificateService service

    @GetMapping("/all-gui")
    ResponseEntity allGUI(){
        return ResponseEntity.ok(service.getAllGUI())
    }

    @PostMapping("/retrieve-from-port")
    ResponseEntity retrieveFromPort(@RequestBody RetrieveFromPortForm retrieveFromPortForm){
        if(retrieveFromPortForm.fromController){
            CertificateLoader certificateLoader = new CertificateLoader()
            List<Certificate> certificates = certificateLoader.loadCertificatesFromHost(retrieveFromPortForm.getHostname(), retrieveFromPortForm.getPort())
            certificates = service.assignIds(certificates)
            ResponseEntity.ok().body(service.toFormGUI(certificates))
        }else{
            List<Certificate> certificates = service.retrieveFromAgent(retrieveFromPortForm)
            certificates = service.assignIds(certificates)
            ResponseEntity.ok().body(service.toFormGUI(certificates))
        }
    }
    /*
    @PostMapping("/retrieve-from-port/download")
    ResponseEntity<Resource> exportAsFile(@RequestBody RetrieveFromPortForm retrieveFromPortForm){
        String filename ="export.cer"
        Resource resource = service.exportAsPem(certificateId, filename)

        try{
            LOG.info("Filename on response is:" + filename)
            ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"").body(resource)
        }catch(Exception exception){
            LOG.error("Error while exporting file: " + exception)
        }

    }*/





}
