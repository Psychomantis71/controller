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

    List<String> keyUsage

    List<String> enhancedKeyUsage

    String alternativeNameDNS

    String alternativeNameIP

    Integer basicConstraints

    Boolean privateKey

    Boolean managed

    Long signerId
}
