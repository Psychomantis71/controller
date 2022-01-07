package eu.outerheaven.certmanager.controller.dto

import eu.outerheaven.certmanager.controller.entity.CertificateType

class StandaloneCertificateDto {

    Long id

    CertificateDto certificateDto

    Long agentId

    String alias

    String path

    CertificateType certificateType

    String password
}
