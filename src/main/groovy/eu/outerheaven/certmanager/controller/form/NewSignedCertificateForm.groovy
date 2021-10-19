package eu.outerheaven.certmanager.controller.form

import groovy.transform.ToString

@ToString(includeFields = true)
class NewSignedCertificateForm {

    String commonName
    String organizationalUnit
    String organization
    String locality
    String stateOrProvinceName
    String countryName
    String emailAddress
    String certAlias
    String keyAlgorithm
    String signatureAlgorithm
    Long keySize
    String dateFrom
    String dateTo
    Long signingCertId
    Boolean intermediate
    String dnsname
    String ipaddres

}
