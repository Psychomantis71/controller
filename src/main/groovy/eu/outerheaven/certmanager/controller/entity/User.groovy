package eu.outerheaven.certmanager.controller.entity

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id

@Entity
class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id

    private String userName

    private String password

    private UserRole userRole

    private String email

    int getId() {
        return id
    }

    void setId(Long id) {
        this.id = id
    }

    String getUserName() {
        return userName
    }

    void setUserName(String userName) {
        this.userName = userName
    }

    String getPassword() {
        return password
    }

    void setPassword(String password) {
        this.password = password
    }

    UserRole getUserRole() {
        return userRole
    }

    void setUserRole(UserRole userRole) {
        this.userRole = userRole
    }

    String getEmail() {
        return email
    }

    void setEmail(String email) {
        this.email = email
    }
}
