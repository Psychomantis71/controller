package eu.outerheaven.certmanager.controller.dto

import java.security.Key

class CertificateDto {

    Long id

    String encodedX509Certificate

    String encodedPrivateKey

}
