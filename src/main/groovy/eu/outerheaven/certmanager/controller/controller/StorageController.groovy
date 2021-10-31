package eu.outerheaven.certmanager.controller.controller

import eu.outerheaven.certmanager.controller.form.PayloadLocationFormGUI
import eu.outerheaven.certmanager.controller.form.PayloadUploadForm
import eu.outerheaven.certmanager.controller.service.StorageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/files")
class StorageController {

    @Autowired
    private final StorageService service


    @GetMapping("/all-gui")
    ResponseEntity allGUI(){
        return ResponseEntity.ok(service.fetchAllLocations())
    }

    @PostMapping("/add-location")
    ResponseEntity addLocation(@RequestBody PayloadLocationFormGUI payloadLocationFormGUI){
        return ResponseEntity.ok(service.addLocation(payloadLocationFormGUI))
    }

    @DeleteMapping("/remove-location")
    ResponseEntity removeLocation(@RequestBody List<PayloadLocationFormGUI> payloadLocationFormGUIS){
        return ResponseEntity.ok(service.removeLocations(payloadLocationFormGUIS))
    }

    @PostMapping("/upload-file")
    ResponseEntity uploadFileToAgent(@RequestBody PayloadUploadForm payloadUploadForm){
        return ResponseEntity.ok(service.uploadFile(payloadUploadForm))
    }
}
