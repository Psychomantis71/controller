package eu.outerheaven.certmanager.controller.controller

import eu.outerheaven.certmanager.controller.dto.KeystoreDto
import eu.outerheaven.certmanager.controller.entity.Certificate
import eu.outerheaven.certmanager.controller.form.KeystoreForm
import eu.outerheaven.certmanager.controller.form.KeystoreFormGUI
import eu.outerheaven.certmanager.controller.form.RetrieveFromPortForm
import eu.outerheaven.certmanager.controller.service.KeystoreService
import eu.outerheaven.certmanager.controller.util.CertificateLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/api/keystore")
class KeystoreController {

    private final Logger LOG = LoggerFactory.getLogger(InstanceController)

    @Autowired
    private final KeystoreService service

    @GetMapping("/all-gui")
    ResponseEntity allGUI(){
        return ResponseEntity.ok(service.getAllGUI())
    }

    @PostMapping("/add")
    ResponseEntity add(@RequestBody ArrayList<KeystoreForm> keystoreForms){
        return ResponseEntity.ok(service.add(keystoreForms))
    }

    @PostMapping("/update")
    ResponseEntity update(@RequestBody KeystoreDto keystoreDto, HttpServletRequest request){
        String username = SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        LOG.info("Request arrived with username:" + username)
        String ip = request.getRemoteAddr()
        service.updateKeystore(keystoreDto,username,ip)
        return ResponseEntity.ok("OK")
    }

    @PostMapping("/remove")
    ResponseEntity delete(@RequestBody List<KeystoreFormGUI> keystoreFormGUIS ){
        return ResponseEntity.ok(service.delete(keystoreFormGUIS))
    }

    @PostMapping("/retrieve-port")
    ResponseEntity retrieveFromPort(@RequestBody RetrieveFromPortForm retrieveFromPortForm){
        if(retrieveFromPortForm.instanceId==0){
            CertificateLoader certificateLoader = new CertificateLoader()
            List<Certificate> certificates = certificateLoader.loadCertificatesFromHost(retrieveFromPortForm.getHostname(), retrieveFromPortForm.getPort())
        }else{

        }
    }



}
