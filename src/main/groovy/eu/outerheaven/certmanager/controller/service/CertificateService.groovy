package eu.outerheaven.certmanager.controller.service

import eu.outerheaven.certmanager.controller.entity.Certificate
import eu.outerheaven.certmanager.controller.entity.Instance
import eu.outerheaven.certmanager.controller.entity.Keystore
import eu.outerheaven.certmanager.controller.form.CertificateFormGUI
import eu.outerheaven.certmanager.controller.form.KeystoreForm
import eu.outerheaven.certmanager.controller.repository.CertificateRepository
import eu.outerheaven.certmanager.controller.repository.InstanceRepository
import eu.outerheaven.certmanager.controller.repository.KeystoreRepository
import eu.outerheaven.certmanager.controller.util.PreparedRequest
import eu.outerheaven.certmanager.controller.util.deserializers.X509CertificateDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

import java.security.cert.X509Certificate

@Service
class CertificateService {

    private final Logger LOG = LoggerFactory.getLogger(CertificateService)

    @Autowired
    private final KeystoreRepository keystoreRepository

    @Autowired
    private final InstanceRepository instanceRepository

    @Autowired
    private final CertificateRepository repository

    String api_url="/api/certificate"

    CertificateFormGUI toFormGUI(Certificate certificate){

        Keystore keystore = keystoreRepository.findById(certificate.getKeystoreId()).get()
        Instance instance = instanceRepository.findById(keystore.getInstanceId()).get()

        CertificateFormGUI certificateFormGUI = new CertificateFormGUI(
                id: certificate.id,
                alias: certificate.alias,
                keystorePath: keystore.location,
                instanceName: instance.name,
                hostname: instance.hostname,
                managed: certificate.managed,
                subject: certificate.getX509Certificate().subjectDN,
                issuer: certificate.getX509Certificate().issuerDN,
                validFrom: certificate.getX509Certificate().notBefore,
                validTo: certificate.getX509Certificate().notAfter,
                serial: certificate.getX509Certificate().serialNumber
        )
        return certificateFormGUI
    }

    ArrayList<CertificateFormGUI> toFormGUI(ArrayList<Certificate> certificates){
        ArrayList<CertificateFormGUI> certificateFormGUIS = new ArrayList<>()
        certificates.forEach(r ->certificateFormGUIS.add(toFormGUI(r)))
        return certificateFormGUIS
    }

    ArrayList<CertificateFormGUI> getAllGUI(){
        ArrayList<Certificate> certificates = repository.findAll() as ArrayList<Certificate>
        ArrayList<CertificateFormGUI> certificateFormGUIS = toFormGUI(certificates)
        return certificateFormGUIS
    }

    void fetchCert(){
        /*PreparedRequest preparedRequest = new PreparedRequest()
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<Keystore> request = new HttpEntity<>(keystore, preparedRequest.getHeader(instance))



        Keystore keystore = restTemplate.getForObject("http://192.168.1.23:8100/api/keystore" + "/1", Keystore.class);
        restTemplate.getForObject("http://192.168.1.23:8100/api/keystore" + "/1",Keystore.class,request)
        ResponseEntity<Keystore> response = restTemplate.exchange(
                "http://192.168.1.23:8100/api/keystore" + "/1",
                HttpMethod.POST,
                request,
                Keystore.class
        );

         */

    }
}
