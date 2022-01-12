package eu.outerheaven.certmanager.controller.repository

import eu.outerheaven.certmanager.controller.entity.Certificate
import org.springframework.data.repository.CrudRepository

import java.security.cert.X509Certificate

interface CertificateRepository extends CrudRepository<Certificate, Long>{

    Certificate findByX509Certificate(X509Certificate x509Certificate)

    List<Certificate> findBySignerCertificateId(Long signerId)

    List<Certificate> findByManaged(boolean managed)
}