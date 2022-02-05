package eu.outerheaven.certmanager.controller.service

import eu.outerheaven.certmanager.controller.entity.Certificate
import eu.outerheaven.certmanager.controller.form.CertificateFormGUI
import eu.outerheaven.certmanager.controller.repository.CertificateRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service

import java.security.cert.CertificateParsingException
import java.security.cert.X509Certificate
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
                keyUsage: getKeyUsageList(certificate.x509Certificate),
                enhancedKeyUsage: certificate.x509Certificate.getExtendedKeyUsage(),
                alternativeNameDNS: getAlternateNames(certificate.x509Certificate,2),
                alternativeNameIP: getAlternateNames(certificate.x509Certificate,7),
                basicConstraints: certificate.x509Certificate.basicConstraints,
                privateKey: pk
        )
        return certificateFormGUI
    }

    List<CertificateFormGUI> toFormGUI(List<Certificate> certificates){
        List<CertificateFormGUI> certificateFormGUIS = new ArrayList<>()
        certificates.forEach(r->{
            certificateFormGUIS.add(toFormGUI(r))
        })
        return certificateFormGUIS
    }

    List<CertificateFormGUI> getAllGUI(){
        List<Certificate> certificates = repository.findAll() as ArrayList<Certificate>
        List<CertificateFormGUI> certificateFormGUIS = toFormGUI(certificates)
        return certificateFormGUIS
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

    protected String getAlternateNames(final X509Certificate cert, Integer targetType) {
        final StringBuilder res = new StringBuilder();

        try {
            if (cert.getSubjectAlternativeNames() == null) {
                return null;
            }

            for (List<?> entry : cert.getSubjectAlternativeNames()) {
                final int type = ((Integer) entry.get(0)).intValue();

                // DNS or IP
                if (type == targetType) {
                    if (res.length() > 0) {
                        res.append(", ");
                    }

                    res.append(entry.get(1));
                }
            }
        } catch (CertificateParsingException ex) {
            // Do nothing
        }

        return res.toString();
    }

    //TODO ask someone for a more efficient way to do this, this is stupid
    List<String> getKeyUsageList(X509Certificate certificate){
        List<String> keyUsageList = new ArrayList<>()
        boolean[] keyUsage = certificate.getKeyUsage()
        if(keyUsage[0]) keyUsageList.add(new String("digitalSignature"))
        if(keyUsage[1]) keyUsageList.add(new String("nonRepudiation"))
        if(keyUsage[2]) keyUsageList.add(new String("keyEncipherment"))
        if(keyUsage[3]) keyUsageList.add(new String("dataEncipherment"))
        if(keyUsage[4]) keyUsageList.add(new String("keyAgreement"))
        if(keyUsage[5]) keyUsageList.add(new String("keyCertSign"))
        if(keyUsage[6]) keyUsageList.add(new String("cRLSign"))
        if(keyUsage[7]) keyUsageList.add(new String("encipherOnly"))
        if(keyUsage[8]) keyUsageList.add(new String("decipherOnly"))

        return keyUsageList
    }


}
