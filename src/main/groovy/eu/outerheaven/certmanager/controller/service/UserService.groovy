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
