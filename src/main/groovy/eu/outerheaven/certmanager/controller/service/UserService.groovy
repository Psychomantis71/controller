package eu.outerheaven.certmanager.controller.service

import eu.outerheaven.certmanager.controller.entity.User
import eu.outerheaven.certmanager.controller.entity.UserRole
import eu.outerheaven.certmanager.controller.form.UserForm
import eu.outerheaven.certmanager.controller.repository.UserRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService {
    private static BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder()
    private static final Logger LOG = LoggerFactory.getLogger(UserService)

    @Autowired
    private final UserRepository repository

    @Autowired
    private final TotpManager totpManager

    List<UserForm> getAll(){
        /*
        List<User> admins = repository.findByUserRole(UserRole.ADMIN)
        List<User> users = repository.findByUserRole(UserRole.USER)
        List<User> recipients = repository.findByUserRole(UserRole.RECIPIENT)
        */

        List<User> users = repository.findByUserRole(UserRole.ADMIN) + repository.findByUserRole(UserRole.USER) + repository.findByUserRole(UserRole.RECIPIENT)

        List<UserForm> usersGUI = toForm(users)
        return usersGUI
    }

    void addUser(UserForm userForm){
        User user = repository.findByUserName(userForm.getUsername())
        if(user != null){
            LOG.error("Cannot create new user with that a username that already exists!")
            throw new Exception("User with such username already exists!")
        }
        User newUser = toClass(userForm)
        repository.save(newUser)
    }

    void removeUser(List<UserForm> userForms){
        userForms.forEach(r->repository.deleteById(r.getId()))
    }

    void changePassword(Long userId, User requester, UserForm userForm){
        User user = repository.findById(userId).get()
        if(user != requester || requester.userRole != UserRole.ADMIN){
            LOG.error("A user tried to change a password of another user, yet it isnt his own password and the user is not a administrator")
            throw new Exception("Denied password change")
        }else{
            user.setPassword(passwordEncoder.encode(userForm.password))
            repository.save(user)
            LOG.info("User {} just changed the password of the user {}",requester.userName,user.userName)
        }
    }


    void changeEmail(Long userId, User requester, UserForm userForm){
        User user = repository.findById(userId).get()
        if(user != requester || requester.userRole != UserRole.ADMIN){
            LOG.error("A user tried to change a email of another user, yet it isnt his own email and the user is not a administrator")
            throw new Exception("Denied email change")
        }else{
            user.setEmail(userForm.email)
            repository.save(user)
            LOG.info("User {} just changed the email of the user {}",requester.userName,user.userName)
        }
    }

    String enable2Fa(Long userId, User requester){
        User user = repository.findById(userId).get()
        if(user != requester || requester.userRole != UserRole.ADMIN){
            LOG.error("A user tried to change a email of another user, yet it isnt his own email and the user is not a administrator")
            throw new Exception("Denied email change")
        }else{
            user.setSecret(totpManager.generateSecret())
            user.setMfa(true)
            repository.save(user)
            LOG.info("For the user {} two factor authentication has been enabled by user {}", user.userName, requester.userName)
            return totpManager.getUriForImage(user.getSecret())
        }
    }

    boolean validateOtp(Long userId, User requester, String otpCode){
        User user = repository.findById(userId).get()
        if(user != requester || requester.userRole != UserRole.ADMIN){
            LOG.error("A user tried to change a email of another user, yet it isnt his own email and the user is not a administrator")
            throw new Exception("Denied email change")
        }else{
            return totpManager.verifyCode(otpCode,user.getSecret())
        }
    }

    UserForm getUserData(User user){
        UserForm userForm = toForm(user)
        return userForm
    }

    User toClass(UserForm userForm){
        User user
        if(userForm.id == null ){
            user = new User(
                    userName: userForm.username,
                    password: passwordEncoder.encode(userForm.password),
                    userRole: userForm.userRole,
                    email: userForm.email
            )
        }else{
            user = new User(
                    id: userForm.id,
                    userName: userForm.username,
                    password: passwordEncoder.encode(userForm.password),
                    userRole: userForm.userRole,
                    email: userForm.email
            )
        }

        return user
    }

    List<User> toClass(List<UserForm> userForms){
        List<User> users = new ArrayList<>()
        userForms.forEach(r->users.add(toClass(r)))
        return  users
    }

    UserForm toForm(User user){
        UserForm userForm = new UserForm(
                id: user.id,
                username: user.userName,
                userRole: user.userRole,
                email: user.email
        )
        return userForm
    }

    List<UserForm> toForm(List<User> users){
        List<UserForm> userForms = new ArrayList<>()
        users.forEach(r->userForms.add(toForm(r)))
        return userForms
    }



}
