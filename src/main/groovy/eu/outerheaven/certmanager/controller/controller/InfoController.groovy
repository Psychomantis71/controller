package eu.outerheaven.certmanager.controller.controller

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/info")
class InfoController {

    private final Logger log = LoggerFactory.getLogger(InfoController)

    @GetMapping("/ping")
    ResponseEntity ping(){
        return ResponseEntity.ok().body("pong")
    }

    @GetMapping("/version")
    ResponseEntity version(){
        return ResponseEntity.ok().body("OSCM Controller version: 0.0.1")
    }


}
