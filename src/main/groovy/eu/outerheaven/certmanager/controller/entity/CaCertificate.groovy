package eu.outerheaven.certmanager.controller.entity

import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import java.security.Key
import java.security.PrivateKey
import java.security.cert.X509Certificate

@Entity
class CaCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id

    @ManyToOne(cascade = CascadeType.ALL)
    private Certificate certificate

    private String alias

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

    String getAlias() {
        return alias
    }

    void setAlias(String alias) {
        this.alias = alias
    }
}
