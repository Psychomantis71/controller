package eu.outerheaven.certmanager.controller.controller

import eu.outerheaven.certmanager.controller.dto.CertificateDto
import eu.outerheaven.certmanager.controller.dto.CertificateImportDto
import eu.outerheaven.certmanager.controller.form.KeystoreCertificateFormGUI
import eu.outerheaven.certmanager.controller.form.KeystoreForm
import eu.outerheaven.certmanager.controller.service.CaVaultService
import eu.outerheaven.certmanager.controller.service.KeystoreCertificateService
import eu.outerheaven.certmanager.controller.util.PreparedRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/api/certificate")
class KeystoreCertificateController {

    private final Logger LOG = LoggerFactory.getLogger(KeystoreCertificateController)

    @Autowired
    private final KeystoreCertificateService service

    @Autowired
    private final CaVaultService caVaultService

    @GetMapping("/all-gui")
    ResponseEntity allGUI(){
        return ResponseEntity.ok(service.getAllGUI())
    }

    @PostMapping("/add")
    ResponseEntity add(@RequestBody ArrayList<KeystoreForm> keystoreForms){
        return ResponseEntity.ok("OK")
    }

    @PutMapping("/update")
    ResponseEntity update(@RequestBody List<CertificateDto> certificateDtos, HttpServletRequest request){
        PreparedRequest preparedRequest = new PreparedRequest()
        LOG.info("Request for update from IP:" + preparedRequest.getClientIpAddressIfServletRequestExist(request).toString())
        //service.update(certificateDtos, request)
        return ResponseEntity.ok("OK")
    }

    @PostMapping("/remove")
    ResponseEntity delete(@RequestBody List<KeystoreCertificateFormGUI> certificateFormGUIS){
        ResponseEntity.ok(service.delete(certificateFormGUIS))
    }

    @GetMapping("/test-generation")
    ResponseEntity testGen(){
        ResponseEntity.ok(caVaultService.main())
    }

    @GetMapping("/{certificateId}/export-pem")
    ResponseEntity<Resource> exportAsFile(@PathVariable Long certificateId){

        LOG.info("Export pem controller called")
        String filename = service.getCleanCertName(certificateId)
        Resource resource = service.exportAsPem(certificateId, filename)

        try{
            LOG.info("Filename on response is:" + filename)
            ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"").body(resource)
        }catch(Exception exception){
            LOG.error("Error while exporting file: " + exception)
        }

    }


    @PostMapping("/import")
    ResponseEntity importCert(@RequestBody CertificateImportDto certificateImportDto){
        ResponseEntity.ok(service.importCertificate(certificateImportDto))
    }

    @PostMapping("/{signerCertificateId}/assign-signer")
    ResponseEntity assignSigner(@PathVariable Long signerCertificateId, @RequestBody List<KeystoreCertificateFormGUI> certificateFormGUIS){
        service.assignSignerCert(signerCertificateId, certificateFormGUIS)
        ResponseEntity.ok().body("")
    }

}
