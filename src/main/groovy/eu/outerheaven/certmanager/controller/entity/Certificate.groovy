package eu.outerheaven.certmanager.controller.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import java.security.PrivateKey
import java.security.cert.X509Certificate

@Entity
class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id

    @Column(length = 4000)
    private X509Certificate x509Certificate

    @Column(length = 8192)
    private PrivateKey privateKey

    private Long signerCertificateId

    private boolean managed

    Long getId() {
        return id
    }

    void setId(Long id) {
        this.id = id
    }

    X509Certificate getX509Certificate() {
        return x509Certificate
    }

    void setX509Certificate(X509Certificate x509Certificate) {
        this.x509Certificate = x509Certificate
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

    boolean getManaged() {
        return managed
    }

    void setManaged(boolean managed) {
        this.managed = managed
    }
}
