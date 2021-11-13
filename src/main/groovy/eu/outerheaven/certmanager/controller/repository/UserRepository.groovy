package eu.outerheaven.certmanager.controller.repository

import eu.outerheaven.certmanager.controller.entity.User
import eu.outerheaven.certmanager.controller.entity.UserRole
import org.springframework.data.repository.CrudRepository

interface UserRepository extends CrudRepository<User, Long> {

    User findByUserName(String userName)

    List<User> findByUserRole(UserRole userRole)

}
