package eu.outerheaven.certmanager.controller.service

import eu.outerheaven.certmanager.controller.entity.CaCertificate
import eu.outerheaven.certmanager.controller.form.CaCertificateForm
import eu.outerheaven.certmanager.controller.form.CaCertificateFormGUI
import eu.outerheaven.certmanager.controller.form.CertificateFormGUI
import eu.outerheaven.certmanager.controller.form.NewSignedCertificateForm
import eu.outerheaven.certmanager.controller.repository.CaCertificateRepository
import eu.outerheaven.certmanager.controller.repository.CertificateRepository
import org.bouncycastle.asn1.ASN1Encodable
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import org.bouncycastle.util.encoders.Base64
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.security.*
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class CaVaultService {

    private final Logger LOG = LoggerFactory.getLogger(CaVaultService)

    @Autowired
    private final CaCertificateRepository repository

    private static final String BC_PROVIDER = "BC"
    private static final String KEY_ALGORITHM = "RSA"
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA"

    void createRootCert(CaCertificateForm caCertificateForm){
        LOG.info("Starting the creation of new root cert")
        Security.addProvider(new BouncyCastleProvider())

        // Initialize a new KeyPair generator
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(caCertificateForm.getKeyAlgorithm(), BC_PROVIDER)
        keyPairGenerator.initialize(caCertificateForm.getKeySize().toInteger())
        // Setup start date to yesterday and end date for 1 year validity
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-M-dd")

        Date startDate = formatter.parse(caCertificateForm.getDateFrom())

        Date endDate = formatter.parse(caCertificateForm.getDateTo())

        // First step is to create a root certificate
        // First Generate a KeyPair,
        // then a random serial number
        // then generate a certificate using the KeyPair
        KeyPair rootKeyPair = keyPairGenerator.generateKeyPair()
        BigInteger rootSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()))

        // Issued By and Issued To same for root certificate
        X500Name rootCertIssuer = new X500Name("CN=" + caCertificateForm.getCommonName())
        X500Name rootCertSubject = rootCertIssuer
        ContentSigner rootCertContentSigner = new JcaContentSignerBuilder(caCertificateForm.getSignatureAlgorithm()).setProvider(BC_PROVIDER).build(rootKeyPair.getPrivate())
        X509v3CertificateBuilder rootCertBuilder = new JcaX509v3CertificateBuilder(rootCertIssuer, rootSerialNum, startDate, endDate, rootCertSubject, rootKeyPair.getPublic())

        // Add Extensions
        // A BasicConstraint to mark root certificate as CA certificate
        JcaX509ExtensionUtils rootCertExtUtils = new JcaX509ExtensionUtils()
        rootCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true))
        rootCertBuilder.addExtension(Extension.subjectKeyIdentifier, false, rootCertExtUtils.createSubjectKeyIdentifier(rootKeyPair.getPublic()))

        // Create a cert holder and export to X509Certificate
        X509CertificateHolder rootCertHolder = rootCertBuilder.build(rootCertContentSigner)
        X509Certificate rootCert = new JcaX509CertificateConverter().setProvider(BC_PROVIDER).getCertificate(rootCertHolder)

        CaCertificate caCertificate = new CaCertificate(
                alias: caCertificateForm.getCertAlias(),
                privateKey: rootKeyPair.getPrivate(),
                x509Certificate: rootCert,
                managed: false

        )
        repository.save(caCertificate)
        LOG.info("Saved new root cert")
        //writeCertToFileBase64Encoded(rootCert, "root-cert.cer")
        //exportKeyPairToKeystoreFile(rootKeyPair, rootCert, caCertificateForm.getAlias(), "root-cert.pfx", "PKCS12", "pass")

    }

    List<CaCertificateFormGUI> getAllCaCertsGUI(){
        List<CaCertificate> all = repository.findAll() as List<CaCertificate>
        return toFormGUI(all)
    }

    void createSignedCert(NewSignedCertificateForm newSignedCertificateForm){

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(newSignedCertificateForm.keyAlgorithm, BC_PROVIDER)
        keyPairGenerator.initialize(newSignedCertificateForm.keySize.toInteger())

        CaCertificate parentCert = repository.findById(newSignedCertificateForm.signingCertId).get()

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-M-dd")

        Date startDate = formatter.parse(newSignedCertificateForm.getDateFrom())

        Date endDate = formatter.parse(newSignedCertificateForm.getDateTo())


        LOG.info("Recieved request: " + newSignedCertificateForm.toString())


        // Generate a new KeyPair and sign it using the Root Cert Private Key
        // by generating a CSR (Certificate Signing Request)
        String issuedSubject = "CN=" + newSignedCertificateForm.commonName

        if(newSignedCertificateForm.organization != null && newSignedCertificateForm.organization != ""){
            issuedSubject=issuedSubject + ",O="  + newSignedCertificateForm.organization
        }
        if(newSignedCertificateForm.organizationalUnit != null && newSignedCertificateForm.organizationalUnit != ""){
            issuedSubject=issuedSubject +",OU=" +newSignedCertificateForm.organizationalUnit
        }
        if(newSignedCertificateForm.locality != null && newSignedCertificateForm.locality != ""){
            issuedSubject=issuedSubject + ",L="  + newSignedCertificateForm.locality
        }
        if(newSignedCertificateForm.stateOrProvinceName != null && newSignedCertificateForm.stateOrProvinceName != ""){
            issuedSubject=issuedSubject + ",ST="  + newSignedCertificateForm.stateOrProvinceName
        }
        if(newSignedCertificateForm.countryName != null && newSignedCertificateForm.countryName != ""){
            issuedSubject=issuedSubject + ",C="  + newSignedCertificateForm.countryName
        }
        X500Name issuedCertSubject = new X500Name(issuedSubject)
        BigInteger issuedCertSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()))
        KeyPair issuedCertKeyPair = keyPairGenerator.generateKeyPair()

        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(issuedCertSubject, issuedCertKeyPair.getPublic())
        JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder(newSignedCertificateForm.signatureAlgorithm).setProvider(BC_PROVIDER)

        // Sign the new KeyPair with the root cert Private Key
        ContentSigner csrContentSigner = csrBuilder.build(parentCert.getPrivateKey())
        PKCS10CertificationRequest csr = p10Builder.build(csrContentSigner)
        // Use the Signed KeyPair and CSR to generate an issued Certificate
        // Here serial number is randomly generated. In general, CAs use
        // a sequence to generate Serial number and avoid collisions
        X500Name parentCertSubject = new X500Name(parentCert.getX509Certificate().subjectDN.toString())
        X509v3CertificateBuilder issuedCertBuilder = new X509v3CertificateBuilder(parentCertSubject, issuedCertSerialNum, startDate, endDate, csr.getSubject(), csr.getSubjectPublicKeyInfo())

        JcaX509ExtensionUtils issuedCertExtUtils = new JcaX509ExtensionUtils()

        // Add Extensions
        // Use BasicConstraints to say that this Cert is not a CA
        if(newSignedCertificateForm.intermediate){
            issuedCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true))
        }else {
            issuedCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false))
        }

        // Add Issuer cert identifier as Extension
        issuedCertBuilder.addExtension(Extension.authorityKeyIdentifier, false, issuedCertExtUtils.createAuthorityKeyIdentifier(parentCert.getX509Certificate()))
        issuedCertBuilder.addExtension(Extension.subjectKeyIdentifier, false, issuedCertExtUtils.createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo()))

        // Add intended key usage extension if needed
        issuedCertBuilder.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.keyEncipherment))

        // Add DNS name is cert is to used for SSL
        issuedCertBuilder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(new ASN1Encodable[] {
                new GeneralName(GeneralName.dNSName, newSignedCertificateForm.getDnsname()),
                new GeneralName(GeneralName.iPAddress, newSignedCertificateForm.getIpaddres())
        }))

        X509CertificateHolder issuedCertHolder = issuedCertBuilder.build(csrContentSigner)
        X509Certificate issuedCert  = new JcaX509CertificateConverter().setProvider(BC_PROVIDER).getCertificate(issuedCertHolder)

        // Verify the issued cert signature against the root (issuer) cert
        issuedCert.verify(parentCert.getX509Certificate().getPublicKey(), BC_PROVIDER)
        if(newSignedCertificateForm.intermediate){
            CaCertificate caCertificate = new CaCertificate(
                    alias: newSignedCertificateForm.certAlias,
                    privateKey: issuedCertKeyPair.getPrivate(),
                    x509Certificate: issuedCert,
                    managed: false
            )
            repository.save(caCertificate)
        }
        //else{

        //}
        writeCertToFileBase64Encoded(issuedCert, "issued-cert.cer")
        exportKeyPairToKeystoreFile(issuedCertKeyPair, issuedCert, "issued-cert", "issued-cert.pfx", "PKCS12", "pass")
        LOG.info("CA signed cert has been created")

    }

    void main(String[] args) throws Exception{
        // Add the BouncyCastle Provider
        Security.addProvider(new BouncyCastleProvider())

        // Initialize a new KeyPair generator
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM, BC_PROVIDER)
        keyPairGenerator.initialize(2048)

        // Setup start date to yesterday and end date for 1 year validity
        Calendar calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, -1)
        Date startDate = calendar.getTime()

        calendar.add(Calendar.YEAR, 1)
        Date endDate = calendar.getTime()

        // First step is to create a root certificate
        // First Generate a KeyPair,
        // then a random serial number
        // then generate a certificate using the KeyPair
        KeyPair rootKeyPair = keyPairGenerator.generateKeyPair()
        BigInteger rootSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()))

        // Issued By and Issued To same for root certificate
        X500Name rootCertIssuer = new X500Name("CN=OSCM-root")
        X500Name rootCertSubject = rootCertIssuer
        ContentSigner rootCertContentSigner = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(BC_PROVIDER).build(rootKeyPair.getPrivate())
        X509v3CertificateBuilder rootCertBuilder = new JcaX509v3CertificateBuilder(rootCertIssuer, rootSerialNum, startDate, endDate, rootCertSubject, rootKeyPair.getPublic())

        // Add Extensions
        // A BasicConstraint to mark root certificate as CA certificate
        JcaX509ExtensionUtils rootCertExtUtils = new JcaX509ExtensionUtils()
        rootCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true))
        rootCertBuilder.addExtension(Extension.subjectKeyIdentifier, false, rootCertExtUtils.createSubjectKeyIdentifier(rootKeyPair.getPublic()))

        // Create a cert holder and export to X509Certificate
        X509CertificateHolder rootCertHolder = rootCertBuilder.build(rootCertContentSigner)
        X509Certificate rootCert = new JcaX509CertificateConverter().setProvider(BC_PROVIDER).getCertificate(rootCertHolder)

        writeCertToFileBase64Encoded(rootCert, "root-cert.cer")
        exportKeyPairToKeystoreFile(rootKeyPair, rootCert, "root-cert", "root-cert.pfx", "PKCS12", "pass")
        LOG.info("CA cert generated and saved")
        // Generate a new KeyPair and sign it using the Root Cert Private Key
        // by generating a CSR (Certificate Signing Request)
        X500Name issuedCertSubject = new X500Name("CN=issued-cert")
        BigInteger issuedCertSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()))
        KeyPair issuedCertKeyPair = keyPairGenerator.generateKeyPair()

        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(issuedCertSubject, issuedCertKeyPair.getPublic())
        JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(BC_PROVIDER)

        // Sign the new KeyPair with the root cert Private Key
        ContentSigner csrContentSigner = csrBuilder.build(rootKeyPair.getPrivate())
        PKCS10CertificationRequest csr = p10Builder.build(csrContentSigner)

        // Use the Signed KeyPair and CSR to generate an issued Certificate
        // Here serial number is randomly generated. In general, CAs use
        // a sequence to generate Serial number and avoid collisions
        X509v3CertificateBuilder issuedCertBuilder = new X509v3CertificateBuilder(rootCertIssuer, issuedCertSerialNum, startDate, endDate, csr.getSubject(), csr.getSubjectPublicKeyInfo())

        JcaX509ExtensionUtils issuedCertExtUtils = new JcaX509ExtensionUtils()

        // Add Extensions
        // Use BasicConstraints to say that this Cert is not a CA
        issuedCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false))

        // Add Issuer cert identifier as Extension
        issuedCertBuilder.addExtension(Extension.authorityKeyIdentifier, false, issuedCertExtUtils.createAuthorityKeyIdentifier(rootCert))
        issuedCertBuilder.addExtension(Extension.subjectKeyIdentifier, false, issuedCertExtUtils.createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo()))

        // Add intended key usage extension if needed
        issuedCertBuilder.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.keyEncipherment))

        // Add DNS name is cert is to used for SSL
        issuedCertBuilder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(new ASN1Encodable[] {
                new GeneralName(GeneralName.dNSName, "raspi.lan"),
                new GeneralName(GeneralName.iPAddress, "192.168.1.203")
        }))

        X509CertificateHolder issuedCertHolder = issuedCertBuilder.build(csrContentSigner)
        X509Certificate issuedCert  = new JcaX509CertificateConverter().setProvider(BC_PROVIDER).getCertificate(issuedCertHolder)

        // Verify the issued cert signature against the root (issuer) cert
        issuedCert.verify(rootCert.getPublicKey(), BC_PROVIDER)

        writeCertToFileBase64Encoded(issuedCert, "issued-cert.cer")
        exportKeyPairToKeystoreFile(issuedCertKeyPair, issuedCert, "issued-cert", "issued-cert.pfx", "PKCS12", "pass")
        LOG.info("CA signed cert has been created")
    }

    void exportKeyPairToKeystoreFile(KeyPair keyPair, Certificate certificate, String alias, String fileName, String storeType, String storePass) throws Exception {
        KeyStore sslKeyStore = KeyStore.getInstance(storeType, BC_PROVIDER)
        sslKeyStore.load(null, null)
        LOG.info("Saved private key is: " + keyPair.getPrivate())
        sslKeyStore.setKeyEntry(alias, keyPair.getPrivate(),null, new Certificate[]{certificate})
        FileOutputStream keyStoreOs = new FileOutputStream(fileName)
        sslKeyStore.store(keyStoreOs, storePass.toCharArray())
        getKeyPairFromKeystore(alias,fileName,storeType,storePass)

    }

    void writeCertToFileBase64Encoded(Certificate certificate, String fileName) throws Exception {
        FileOutputStream certificateOut = new FileOutputStream(fileName)
        String certData = new String(Base64.encode(certificate.getEncoded()))
        certData = certData.replaceAll("(.{67})", "\$1\n")
        certificateOut.write("-----BEGIN CERTIFICATE-----\n".getBytes())
        //certificateOut.write(Base64.encode(certificate.getEncoded()))
        certificateOut.write(certData.getBytes())
        certificateOut.write("\n-----END CERTIFICATE-----".getBytes())
        certificateOut.close()
    }

    void getKeyPairFromKeystore(String alias, String fileName, String storeType, String storePass){
        KeyStore sslKeystore = KeyStore.getInstance(storeType, BC_PROVIDER)
        InputStream inputstream = new FileInputStream(fileName)
        sslKeystore.load(inputstream, storePass.toCharArray())
        Key key = sslKeystore.getKey(alias)
        LOG.info("Read private key is: " + key)
    }

    CaCertificateFormGUI toFormGUI(CaCertificate caCertificate){
        String level
        if(caCertificate.x509Certificate.issuerDN == caCertificate.getX509Certificate().subjectDN){
            level = "ROOT"
        }else{
            level="INTERMEDIATE"
        }
        CaCertificateFormGUI caCertificateFormGUI = new CaCertificateFormGUI(
                id: caCertificate.id,
                alias: caCertificate.alias,
                managed: caCertificate.managed,
                status: certStatus(caCertificate.getX509Certificate().notBefore,caCertificate.getX509Certificate().notAfter),
                level: level,
                subject: caCertificate.getX509Certificate().subjectDN,
                issuer: caCertificate.getX509Certificate().issuerDN,
                validFrom: caCertificate.getX509Certificate().notBefore,
                validTo: caCertificate.getX509Certificate().notAfter,
                serial: caCertificate.getX509Certificate().serialNumber
        )
        return caCertificateFormGUI
    }

    List<CaCertificateFormGUI> toFormGUI(List<CaCertificate> caCertificates){
        List<CaCertificateFormGUI> certificateFormGUIS = new ArrayList<>()
        caCertificates.forEach(r->certificateFormGUIS.add(toFormGUI(r)))
        return certificateFormGUIS
    }

    String certStatus(Date notBefore, Date notAfter){
        String status
        Instant instant = Instant.ofEpochMilli(notBefore.getTime())
        LocalDateTime ldtNotBefore = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        instant = Instant.ofEpochMilli(notAfter.getTime())
        LocalDateTime ldtNotAfter = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        //IF the not before date is after current time, invalid cert
        //TODO optimise order of this
        if(ldtNotBefore.isAfter(LocalDateTime.now())){
            status="NOT YET VALID"
        } else if(ldtNotAfter.isBefore(LocalDateTime.now())){
            //IF not after is before the current date, it is expired
            status="EXPIRED"
        }else if(ldtNotAfter.plusDays(30).isBefore(LocalDateTime.now())){
            //IF not after +30 days is before it will expire soon
            status="EXPIRING SOON"
        }else(status="VALID")

        return status
    }

}
