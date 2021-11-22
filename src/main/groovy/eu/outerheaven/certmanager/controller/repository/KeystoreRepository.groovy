package eu.outerheaven.certmanager.controller.repository

import eu.outerheaven.certmanager.controller.entity.Keystore
import org.springframework.data.repository.CrudRepository

interface KeystoreRepository extends CrudRepository<Keystore, Long> {

    Keystore findByInstanceIdAndAgentId(Long instanceId, Long AgentId)

    List<Keystore> findByInstanceId(Long instanceId)

}