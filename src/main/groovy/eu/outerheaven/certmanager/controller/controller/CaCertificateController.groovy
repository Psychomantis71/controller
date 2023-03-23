package eu.outerheaven.certmanager.controller.controller

import eu.outerheaven.certmanager.controller.dto.CertificateImportDto
import eu.outerheaven.certmanager.controller.form.CaCertificateForm
import eu.outerheaven.certmanager.controller.form.CaCertificateFormGUI
import eu.outerheaven.certmanager.controller.form.NewSignedCertificateForm
import eu.outerheaven.certmanager.controller.service.CaVaultService
import org.bouncycastle.asn1.x509.Certificate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.RequestEntity
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
        ResponseEntity.ok(service.createSignedCACert(newSignedCertificateForm))
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

    @PostMapping("/{certificateId}/replace")
    ResponseEntity replace(@RequestBody CertificateImportDto certificateImportDto, @PathVariable Long certificateId){
        ResponseEntity.ok(service.replaceCertificate(certificateImportDto, certificateId))
    }

    @PostMapping("/remove")
    ResponseEntity remove(@RequestBody List<CaCertificateFormGUI> certificateFormGUIS){
        ResponseEntity.ok(service.remove(certificateFormGUIS))
    }

    @PostMapping("/{signerCertificateId}/cacert-assign-signer")
    ResponseEntity assignSigner(@PathVariable Long signerCertificateId, @RequestBody List<CaCertificateFormGUI> caCertificateFormGUI){
        service.assignSignerCaCert(signerCertificateId, caCertificateFormGUI)
    }

    @PostMapping("/{signerCertificateId}/renew-cacert")
    ResponseEntity renewCaCert(@PathVariable Long signerCertificateId, @RequestBody List<CaCertificateFormGUI> caCertificateFormGUI){
            service.renewCaCertificate(caCertificateFormGUI)
    }

    @PostMapping("/create-and-export-pem")
    ResponseEntity<Resource> exportAsFile(@RequestBody NewSignedCertificateForm newSignedCertificateForm){

        LOG.info("Create and export signed cert called")
        eu.outerheaven.certmanager.controller.entity.Certificate certificate = service.createSignedCert(newSignedCertificateForm)
        Resource resource = service.exportGeneratedCert(certificate,"export.cer")

        try{
            ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + "export.cer" + "\"").body(resource)
        }catch(Exception exception){
            LOG.error("Error while exporting file: " + exception)
        }

    }
}
