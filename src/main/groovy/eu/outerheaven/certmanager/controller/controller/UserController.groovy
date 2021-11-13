package eu.outerheaven.certmanager.controller.controller

import eu.outerheaven.certmanager.controller.form.UserForm
import eu.outerheaven.certmanager.controller.repository.UserRepository
import eu.outerheaven.certmanager.controller.service.UserService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/user")
class UserController {

    private static BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder()

    private static final Logger LOG = LoggerFactory.getLogger(UserController)

    @Autowired
    private UserRepository repository

    @Autowired
    private UserService service

    @GetMapping("/all-gui")
    ResponseEntity allGUI(){
        return ResponseEntity.ok(service.getAll())
    }

    @PostMapping("/add")
    ResponseEntity addUser(@RequestBody UserForm userForm){
        return ResponseEntity.ok(service.addUser(userForm))
    }

    @PostMapping("/delete")
    ResponseEntity addUser(@RequestBody List<UserForm> userForms){
        return ResponseEntity.ok(service.removeUser(userForms))
    }





}
