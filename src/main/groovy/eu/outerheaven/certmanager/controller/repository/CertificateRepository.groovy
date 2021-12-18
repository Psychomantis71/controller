package eu.outerheaven.certmanager.controller.repository

import eu.outerheaven.certmanager.controller.entity.Certificate
import org.springframework.data.repository.CrudRepository

interface CertificateRepository extends CrudRepository<Certificate, Long>{

    List<Certificate> findByKeystoreId(Long keystoreId)

}