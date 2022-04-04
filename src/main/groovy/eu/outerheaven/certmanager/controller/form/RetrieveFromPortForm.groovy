package eu.outerheaven.certmanager.controller.form

import groovy.transform.ToString

@ToString(includeFields = true)
class RetrieveFromPortForm {

    Integer port
    String hostname
    Long instanceId
    Boolean fromController
    Boolean save
    Boolean download
    List<KeystoreFormGUI> keystoreFormGUIS

}
