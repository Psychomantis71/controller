package eu.outerheaven.certmanager.controller.repository

import eu.outerheaven.certmanager.controller.entity.Certificate
import eu.outerheaven.certmanager.controller.entity.StandaloneCertificate
import org.springframework.data.repository.CrudRepository

interface StandaloneCertificateRepository extends CrudRepository<StandaloneCertificate, Long>{
        List<StandaloneCertificate> findByCertificate(Certificate certificate)
}