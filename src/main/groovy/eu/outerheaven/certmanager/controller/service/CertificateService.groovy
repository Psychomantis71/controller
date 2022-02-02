package eu.outerheaven.certmanager.controller.service

import eu.outerheaven.certmanager.controller.entity.Certificate
import eu.outerheaven.certmanager.controller.form.CertificateFormGUI
import eu.outerheaven.certmanager.controller.repository.CertificateRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class CertificateService {

    private final Logger LOG = LoggerFactory.getLogger(CertificateService)

    @Autowired
    private final CertificateRepository repository

    @Autowired
    private final Environment environment

    CertificateFormGUI toFormGUI(Certificate certificate){
        String certstat = certStatus(certificate.x509Certificate.notBefore,certificate.x509Certificate.notAfter)
        RSAPublicKey pub = (RSAPublicKey) certificate.x509Certificate.getPublicKey()
        boolean pk = false
        if(certificate.privateKey != null) pk=true
        CertificateFormGUI certificateFormGUI = new CertificateFormGUI(
                id: certificate.id,
                status: certstat,
                subject: certificate.x509Certificate.subjectDN,
                issuer: certificate.x509Certificate.issuerDN,
                validFrom: certificate.x509Certificate.notBefore,
                validTo: certificate.x509Certificate.notAfter,
                serial: certificate.x509Certificate.serialNumber,
                signature: certificate.x509Certificate.publicKey.getAlgorithm(),
                signatureHashAlgorithm: certificate.x509Certificate.getSigAlgName().toString(),
                keysize: pub.getModulus().bitLength(),
                privateKey: pk
        )
        return certificateFormGUI
    }

    String certStatus(Date notBefore, Date notAfter){
        String status
        Instant instant = Instant.ofEpochMilli(notBefore.getTime())
        LocalDateTime ldtNotBefore = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        instant = Instant.ofEpochMilli(notAfter.getTime())
        LocalDateTime ldtNotAfter = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        if(ldtNotBefore.isAfter(LocalDateTime.now())){
            status="NOT YET VALID"
        } else if(ldtNotAfter.isBefore(LocalDateTime.now())){
            //IF not after is before the current date, it is expired
            status="EXPIRED"
        }else if(ldtNotAfter.plusDays(environment.getProperty("controller.expiration.check.warn.period").toInteger()).isBefore(LocalDateTime.now())){
            status="EXPIRING SOON"
        }else(status="VALID")

        return status
    }
}
