package eu.outerheaven.certmanager.controller.entity

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.OneToMany

@Entity
class Keystore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id

    //ID of keystore on the agent side
    private Long agentId

    //ID of instance under which this keystore is
    private Long instanceId

    private String location

    private String description

    private String password

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "keystoreId")
    private List<Certificate> certificates

    Long getId() {
        return id
    }

    void setId(Long id) {
        this.id = id
    }

    Long getAgentId() {
        return agentId
    }

    void setAgentId(Long agentId) {
        this.agentId = agentId
    }

    Long getInstanceId() {
        return instanceId
    }

    void setInstanceId(Long instanceId) {
        this.instanceId = instanceId
    }

    String getLocation() {
        return location
    }

    void setLocation(String location) {
        this.location = location
    }

    String getDescription() {
        return description
    }

    void setDescription(String description) {
        this.description = description
    }

    String getPassword() {
        return password
    }

    void setPassword(String password) {
        this.password = password
    }

    List<Certificate> getCertificates() {
        return certificates
    }

    void setCertificates(List<Certificate> certificates) {
        this.certificates = certificates
    }

}
