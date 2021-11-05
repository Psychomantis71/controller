package eu.outerheaven.certmanager.controller.controller

import eu.outerheaven.certmanager.controller.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/user")
class UserController {

    private static BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder()

    @Autowired
    private final UserRepository repository



}
