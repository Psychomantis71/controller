package eu.outerheaven.certmanager.controller.repository

import eu.outerheaven.certmanager.controller.entity.User
import org.springframework.data.repository.CrudRepository

interface UserRepository  extends CrudRepository<User, Long> {

}
