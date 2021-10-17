package eu.outerheaven.certmanager.controller.dto

import java.security.Key

class CertificateDto {

    Long id

    Long agent_id

    String alias

    String key

    String encodedX509

    Boolean managed

    Long keystoreId
}
