package eu.outerheaven.certmanager.controller.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import java.security.Key
import java.security.PrivateKey
import java.security.cert.X509Certificate

@Entity
class CaCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id

    private String alias

    @Column(length = 8192)
    private PrivateKey privateKey

    @Column(length = 4000)
    private X509Certificate x509Certificate

    private Boolean managed

    private Long signerCertificateId

    Long getId() {
        return id
    }

    void setId(Long id) {
        this.id = id
    }

    String getAlias() {
        return alias
    }

    void setAlias(String alias) {
        this.alias = alias
    }


    X509Certificate getX509Certificate() {
        return x509Certificate
    }

    void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate
    }

    Boolean getManaged() {
        return managed
    }

    void setManaged(Boolean managed) {
        this.managed = managed
    }

    PrivateKey getPrivateKey() {
        return privateKey
    }

    void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey
    }

    Long getSignerCertificateId() {
        return signerCertificateId
    }

    void setSignerCertificateId(Long signerCertificateId) {
        this.signerCertificateId = signerCertificateId
    }
}
