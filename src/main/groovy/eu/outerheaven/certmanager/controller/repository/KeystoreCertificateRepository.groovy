package eu.outerheaven.certmanager.controller.repository

import eu.outerheaven.certmanager.controller.entity.Certificate
import eu.outerheaven.certmanager.controller.entity.KeystoreCertificate
import org.springframework.data.repository.CrudRepository

interface KeystoreCertificateRepository extends CrudRepository<KeystoreCertificate, Long>{
        List<KeystoreCertificate> findByKeystoreId(Long keystoreId)
        List<KeystoreCertificate> findByCertificate(Certificate certificate)
}