package eu.outerheaven.certmanager.controller.form

import groovy.transform.ToString

@ToString(includeFields = true)
class CaCertificateForm {

    String alias

    String keyAlgorithm

    String signatureAlgorithm

    Long keySize

    Date validFrom

    Date validTo

    String commonName

}
