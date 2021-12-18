package eu.outerheaven.certmanager.controller.repository

import eu.outerheaven.certmanager.controller.entity.Instance
import eu.outerheaven.certmanager.controller.entity.User
import org.springframework.data.repository.CrudRepository

interface InstanceRepository  extends CrudRepository<Instance, Long> {

    Instance findByName(String name)

    Instance findByHostname(String hostname)

    Instance findByIp(String ip)

    Instance findByPortAndIp(Long port, String ip)

    Instance findByPortAndHostname(Long port, String hostname)

    Instance findByUser(User user)

    List<Instance> findAll()

}