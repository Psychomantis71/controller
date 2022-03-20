package eu.outerheaven.certmanager.controller.form

import eu.outerheaven.certmanager.controller.entity.UserRole
import groovy.transform.ToString

@ToString(includeFields = true)
class UserForm {

    Long id

    String username

    String password

    UserRole userRole

    String email

    Boolean twoFactorAuth

}
