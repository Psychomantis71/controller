package eu.outerheaven.certmanager.controller.controller

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TestController {

    private final Logger LOG = LoggerFactory.getLogger(TestController)

    @GetMapping(value = "homepage")
    public ResponseEntity getHome() {
        return ResponseEntity.ok("Welcome to Your home page");
    }

    @GetMapping(value = "onlyforadmin/welcome")
    public ResponseEntity getSecuredAdmin() {

        String username = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        LOG.info("Username with that you arrived is: " + username)

        return ResponseEntity.ok("Welcome to the secured page. Only for Admin");
    }

    @GetMapping(value = "secured/welcome")
    public ResponseEntity getSecured() {
        return ResponseEntity.ok("Welcome to the secured page");
    }


    @PostMapping(value = "secured/postdata")
    public ResponseEntity postData() {
        return ResponseEntity.ok("Data is saved");
    }
}
