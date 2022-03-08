package eu.outerheaven.certmanager.controller.controller

import eu.outerheaven.certmanager.controller.entity.User
import eu.outerheaven.certmanager.controller.form.UserForm
import eu.outerheaven.certmanager.controller.repository.UserRepository
import eu.outerheaven.certmanager.controller.service.UserService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
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
        ResponseEntity.ok(service.getAll())
    }

    @PostMapping("/add")
    ResponseEntity addUser(@RequestBody UserForm userForm){
        ResponseEntity.ok(service.addUser(userForm))
    }

    @PostMapping("/delete")
    ResponseEntity deleteUser(@RequestBody List<UserForm> userForms){
        ResponseEntity.ok(service.removeUser(userForms))
    }

    @GetMapping("/user-data")
    ResponseEntity getUserData(){
        String username = SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        User requester = repository.findByUserName(username)
        ResponseEntity.ok(service.getUserData(requester))
    }

    @PostMapping("/{userId}/change-password")
    ResponseEntity changePass(@PathVariable Long userId, @RequestBody UserForm userForm){
        String username = SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        User requester = repository.findByUserName(username)
        ResponseEntity.ok(service.changePassword(userId, requester, userForm))
    }

    @PostMapping("/{userId}/change-email")
    ResponseEntity changeEmail(@PathVariable Long userId, @RequestBody UserForm userForm){
        String username = SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        User requester = repository.findByUserName(username)
        ResponseEntity.ok(service.changeEmail(userId, requester, userForm))
    }

    @GetMapping("/{userId}/enable2fa")
    ResponseEntity enable2fa(@PathVariable Long userId){
        String username = SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        User requester = repository.findByUserName(username)
        ResponseEntity.ok(service.enable2Fa(userId, requester))
    }

    @PostMapping("/{userId}/validateOtp/{otpCode}")
    ResponseEntity validateOtp(@PathVariable Long userId, @PathVariable String otpCode){
        String username = SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        User requester = repository.findByUserName(username)
        ResponseEntity.ok(service.validateOtp(userId, requester, otpCode))
    }



}
