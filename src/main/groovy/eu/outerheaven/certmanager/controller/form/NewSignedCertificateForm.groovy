package eu.outerheaven.certmanager.controller.form

import groovy.transform.ToString

@ToString(includeFields = true)
class NewSignedCertificateForm {

    String certAlias

    String keyAlgorithm

    String signatureAlgorithm

    Long keySize

    String dateFrom

    String dateTo

    String commonName

    Long parentCertId


}
