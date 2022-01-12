package eu.outerheaven.certmanager.controller.repository

import eu.outerheaven.certmanager.controller.entity.CaCertificate
import eu.outerheaven.certmanager.controller.entity.Certificate
import org.springframework.data.repository.CrudRepository

interface CaCertificateRepository extends CrudRepository<CaCertificate, Long> {
        List<CaCertificate> findByCertificate(Certificate certificate)

}