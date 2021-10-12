package eu.outerheaven.certmanager.controller.dto

class CertificateDto {

    Long id

    Long agent_id

    String alias

    String encodedX509

    Boolean managed

    Long keystoreId
}
