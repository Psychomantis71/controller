package eu.outerheaven.certmanager.controller.dto

import eu.outerheaven.certmanager.controller.entity.KeystoreCertificate


class KeystoreDto {

    Long id
    Long agentId
    Long instanceId
    String location
    String description
    String password
    List<KeystoreCertificateDto> keystoreCertificateDtos
}
