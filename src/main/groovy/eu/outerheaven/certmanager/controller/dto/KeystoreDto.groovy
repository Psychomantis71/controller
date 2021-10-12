package eu.outerheaven.certmanager.controller.dto


class KeystoreDto {

    Long id
    Long agentId
    Long instanceId
    String location
    String description
    String password
    List<CertificateDto> certificates
}
