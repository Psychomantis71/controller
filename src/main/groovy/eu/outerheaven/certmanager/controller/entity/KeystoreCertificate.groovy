package eu.outerheaven.certmanager.controller.entity

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne

@Entity
class KeystoreCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id

    @ManyToOne(cascade = CascadeType.ALL)
    private Certificate certificate

    private Long agentId

    private String alias

    private Long keystoreId

    private Boolean keypair

    Long getId() {
        return id
    }

    void setId(Long id) {
        this.id = id
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

    Long getKeystoreId() {
        return keystoreId
    }

    void setKeystoreId(Long keystoreId) {
        this.keystoreId = keystoreId
    }

    Boolean getKeypair() {
        return keypair
    }

    void setKeypair(Boolean keypair) {
        this.keypair = keypair
    }
}
