package eu.outerheaven.certmanager.controller.entity

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne

@Entity
class StandaloneCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id

    @ManyToOne
    private Instance instance

    @ManyToOne
    private Certificate certificate

    private Long agentId

    private String alias

    private String path

    private CertificateType certificateType

    private String password

    Long getId() {
        return id
    }

    void setId(Long id) {
        this.id = id
    }

    Instance getInstance() {
        return instance
    }

    void setInstance(Instance instance) {
        this.instance = instance
    }

    Certificate getCertificate() {
        return certificate
    }

    void setCertificate(Certificate certificate) {
        this.certificate = certificate
    }

    Long getAgentId() {
        return agentId
    }

    void setAgentId(Long agentId) {
        this.agentId = agentId
    }

    String getAlias() {
        return alias
    }

    void setAlias(String alias) {
        this.alias = alias
    }

    String getPath() {
        return path
    }

    void setPath(String path) {
        this.path = path
    }

    CertificateType getCertificateType() {
        return certificateType
    }

    void setCertificateType(CertificateType certificateType) {
        this.certificateType = certificateType
    }

    String getPassword() {
        return password
    }

    void setPassword(String password) {
        this.password = password
    }
}
