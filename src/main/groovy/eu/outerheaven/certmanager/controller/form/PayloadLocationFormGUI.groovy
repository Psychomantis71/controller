package eu.outerheaven.certmanager.controller.form

import groovy.transform.ToString

@ToString(includeFields = true)
class PayloadLocationFormGUI {

    Long id

    Long agentId

    String pathName

    String path

    Long instanceId

    String instanceName

    String hostname

}
