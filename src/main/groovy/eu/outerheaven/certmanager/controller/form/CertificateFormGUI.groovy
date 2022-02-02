package eu.outerheaven.certmanager.controller.form

class CertificateFormGUI {

    Long id

    String status

    String subject

    String issuer

    Date validFrom

    Date validTo

    String serial

    String signature

    String signatureHashAlgorithm

    Long keysize

    String[] keyUsage

    String[] enhancedKeyUsage

    String[] alternativeName

    String[] basicConstraints

    Boolean privateKey
}
