package eu.outerheaven.certmanager.controller.service

import eu.outerheaven.certmanager.controller.dto.CertificateImportDto
import eu.outerheaven.certmanager.controller.entity.CaCertificate
import eu.outerheaven.certmanager.controller.entity.KeystoreCertificate
import eu.outerheaven.certmanager.controller.entity.StandaloneCertificate
import eu.outerheaven.certmanager.controller.form.CaCertificateForm
import eu.outerheaven.certmanager.controller.form.CaCertificateFormGUI
import eu.outerheaven.certmanager.controller.form.CertificateFormGUI
import eu.outerheaven.certmanager.controller.form.NewSignedCertificateForm
import eu.outerheaven.certmanager.controller.repository.CaCertificateRepository
import eu.outerheaven.certmanager.controller.repository.CertificateRepository
import eu.outerheaven.certmanager.controller.repository.KeystoreCertificateRepository
import eu.outerheaven.certmanager.controller.repository.KeystoreRepository
import eu.outerheaven.certmanager.controller.repository.StandaloneCertificateRepository
import eu.outerheaven.certmanager.controller.util.CertificateLoader
import org.apache.tomcat.util.http.fileupload.FileUtils
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
import org.springframework.core.env.Environment
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.*
import java.security.cert.Certificate
import java.security.cert.CertificateExpiredException
import java.security.cert.CertificateNotYetValidException
import java.security.cert.CertificateParsingException
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

@Service
class CaVaultService {

    private final Logger LOG = LoggerFactory.getLogger(CaVaultService)

    @Autowired
    private final CaCertificateRepository repository

    @Autowired
    private final CertificateRepository certificateRepository

    @Autowired
    private final StandaloneCertificateRepository standaloneCertificateRepository

    @Autowired
    private final KeystoreCertificateRepository keystoreCertificateRepository

    @Autowired
    private final MailService mailService

    @Autowired
    private Environment environment

    private static final String BC_PROVIDER = "BC"
    private static final String KEY_ALGORITHM = "RSA"
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA"

    //refactored
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

        eu.outerheaven.certmanager.controller.entity.Certificate certificate = new eu.outerheaven.certmanager.controller.entity.Certificate(
                privateKey: rootKeyPair.getPrivate(),
                x509Certificate: rootCert,
                managed: false
        )
        //certificateRepository.save(certificate)
        CaCertificate caCertificate = new CaCertificate(
                alias: caCertificateForm.getCertAlias(),
                certificate: certificate

        )
        repository.save(caCertificate)
        LOG.info("Saved new root cert")
        //writeCertToFileBase64Encoded(rootCert, "root-cert.cer")
        //exportKeyPairToKeystoreFile(rootKeyPair, rootCert, caCertificateForm.getAlias(), "root-cert.pfx", "PKCS12", "pass")

    }

    //refactored
    List<CaCertificateFormGUI> getAllCaCertsGUI(){
        List<CaCertificate> all = repository.findAll() as List<CaCertificate>
        return toFormGUI(all)
    }

    //refactored
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
        ContentSigner csrContentSigner = csrBuilder.build(parentCert.getCertificate().getPrivateKey())
        PKCS10CertificationRequest csr = p10Builder.build(csrContentSigner)
        // Use the Signed KeyPair and CSR to generate an issued Certificate
        // Here serial number is randomly generated. In general, CAs use
        // a sequence to generate Serial number and avoid collisions
        X500Name parentCertSubject = new X500Name(parentCert.getCertificate().getX509Certificate().subjectDN.toString())
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
        issuedCertBuilder.addExtension(Extension.authorityKeyIdentifier, false, issuedCertExtUtils.createAuthorityKeyIdentifier(parentCert.getCertificate().getX509Certificate()))
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
        issuedCert.verify(parentCert.getCertificate().getX509Certificate().getPublicKey(), BC_PROVIDER)

        eu.outerheaven.certmanager.controller.entity.Certificate certificate = new eu.outerheaven.certmanager.controller.entity.Certificate(
                privateKey: issuedCertKeyPair.getPrivate(),
                x509Certificate: issuedCert,
                managed: false,
                signerCertificateId: parentCert.getCertificate().getId()
        )

        if(newSignedCertificateForm.intermediate){
            CaCertificate caCertificate = new CaCertificate(
                    alias: newSignedCertificateForm.certAlias,
                    certificate: certificate
            )
            repository.save(caCertificate)
        }
        //else{

        //}
        //writeCertToFileBase64Encoded(issuedCert, "issued-cert.cer")
        //exportKeyPairToKeystoreFile(issuedCertKeyPair, issuedCert, "issued-cert", "issued-cert.pfx", "PKCS12", "pass")
        LOG.info("CA signed cert has been created")

    }

    //Deprecated
    void renewOld(X509Certificate certificate, Boolean cavault, Long signerCertId, Long certificateId){
        LOG.info("Starting renewal process for certificate ID {}, CaVault: {}",certificateId,cavault.toString())
        CaCertificate parentCert = repository.findById(signerCertId).get()
        Date firstDate = certificate.getNotBefore()
        Date secondDate = certificate.getNotAfter()

        long diffInMillies = Math.abs(secondDate.getTime() - firstDate.getTime());
        long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

        Calendar calendar = Calendar.getInstance();
        //TODO REPLACE THIS WITH CONFIG VARIABLE
        calendar.add(Calendar.DATE, diff.toInteger());

        Date startDate = new Date()
        Date endDate = calendar.getTime();
        boolean[] keyUsage = certificate.getKeyUsage()

        try{
            parentCert.getCertificate().getX509Certificate().checkValidity(endDate)
        }catch(CertificateExpiredException ignored){
            LOG.error("Unable to renew managed certificate with ID {}, the new notAfter date would be after the expiration date of the signer certificate assigned to it! ",certificateId)
            throw new Exception("ASSIGN A SIGNER CERTIFICATE THAT WILL BE VALID AT THE NEW NOTAFTER DATE FOR THE ISSUED CERTIFICATE")
        }catch(CertificateNotYetValidException exception){
            LOG.debug("Exception: ", exception)
            throw new Exception("SIGNER CERTIFICATE NOT YET VALID")
        }


        //KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(certificate.getSigAlgName().toString(), BC_PROVIDER)
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(certificate.getPublicKey().getAlgorithm(), BC_PROVIDER)
        //CAST public key to RSA public to get modulus/keysize
        RSAPublicKey pub = (RSAPublicKey) certificate.getPublicKey()
        keyPairGenerator.initialize(pub.getModulus().bitLength())

        // Generate a new KeyPair and sign it using the Root Cert Private Key
        // by generating a CSR (Certificate Signing Request)
        String issuedSubject = certificate.subjectDN.toString()

        X500Name issuedCertSubject = new X500Name(issuedSubject)
        BigInteger issuedCertSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()))
        KeyPair issuedCertKeyPair = keyPairGenerator.generateKeyPair()

        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(issuedCertSubject, issuedCertKeyPair.getPublic())
        JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder(certificate.getSigAlgName().toString()).setProvider(BC_PROVIDER)

        // Sign the new KeyPair with the root cert Private Key
        ContentSigner csrContentSigner = csrBuilder.build(parentCert.getCertificate().getPrivateKey())
        PKCS10CertificationRequest csr = p10Builder.build(csrContentSigner)
        // Use the Signed KeyPair and CSR to generate an issued Certificate
        // Here serial number is randomly generated. In general, CAs use
        // a sequence to generate Serial number and avoid collisions
        X500Name parentCertSubject = new X500Name(parentCert.getCertificate().getX509Certificate().subjectDN.toString())
        X509v3CertificateBuilder issuedCertBuilder = new X509v3CertificateBuilder(parentCertSubject, issuedCertSerialNum, startDate, endDate, csr.getSubject(), csr.getSubjectPublicKeyInfo())

        JcaX509ExtensionUtils issuedCertExtUtils = new JcaX509ExtensionUtils()

        // Add Extensions
        // Use BasicConstraints to say that this Cert is not a CA
        if(keyUsage[5]){
            issuedCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true))
        }else {
            issuedCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false))
        }

        // Add Issuer cert identifier as Extension
        issuedCertBuilder.addExtension(Extension.authorityKeyIdentifier, false, issuedCertExtUtils.createAuthorityKeyIdentifier(parentCert.getCertificate().getX509Certificate()))
        issuedCertBuilder.addExtension(Extension.subjectKeyIdentifier, false, issuedCertExtUtils.createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo()))

        // Add intended key usage extension if needed
        issuedCertBuilder.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.keyEncipherment))

        // Add DNS name is cert is to used for SSL
        if(certificate.getSubjectAlternativeNames().getAt(2).toString() != null && certificate.getSubjectAlternativeNames().getAt(7).toString() != null ){
            issuedCertBuilder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(new ASN1Encodable[] {
                    new GeneralName(GeneralName.dNSName, getAlternateNames(certificate,2)),
                    new GeneralName(GeneralName.iPAddress, getAlternateNames(certificate,7))
            }))
        }else if(certificate.getSubjectAlternativeNames().getAt(2)!= null){
            issuedCertBuilder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(new ASN1Encodable[] {
                    new GeneralName(GeneralName.dNSName, getAlternateNames(certificate,2)),
            }))
        }else if(certificate.getSubjectAlternativeNames().getAt(7) != null){
            issuedCertBuilder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(new ASN1Encodable[] {
                    new GeneralName(GeneralName.iPAddress, getAlternateNames(certificate,7))
            }))
        }

        X509CertificateHolder issuedCertHolder = issuedCertBuilder.build(csrContentSigner)
        X509Certificate issuedCert  = new JcaX509CertificateConverter().setProvider(BC_PROVIDER).getCertificate(issuedCertHolder)

        // Verify the issued cert signature against the root (issuer) cert
        issuedCert.verify(parentCert.getCertificate().getX509Certificate().getPublicKey(), BC_PROVIDER)
        //writeCertToFileBase64Encoded(issuedCert, "re-issued-cert.cer")
        //exportKeyPairToKeystoreFile(issuedCertKeyPair, issuedCert, "re-issued-cert", "issued-cert.pfx", "PKCS12", "password")
        if(cavault){
            CaCertificate tosave = repository.findById(certificateId).get()
            eu.outerheaven.certmanager.controller.entity.Certificate cert
            tosave.setPrivateKey(issuedCertKeyPair.getPrivate())
            tosave.setX509Certificate(issuedCert)
            repository.save(tosave)
        }else{
            LOG.info("Well fuck not implemented yet")
        }
        LOG.info("Finished renewal process!")
    }

    //Refactored
    void renew(eu.outerheaven.certmanager.controller.entity.Certificate certificateToRenew){
        LOG.info("Starting renewal process for certificate with ID ", certificateToRenew.getId())
        CaCertificate parentCert = repository.findById(certificateToRenew.getSignerCertificateId()).get()
        X509Certificate certificate = certificateToRenew.getX509Certificate()
        Date firstDate = certificate.getNotBefore()
        Date secondDate = certificate.getNotAfter()

        long diffInMillies = Math.abs(secondDate.getTime() - firstDate.getTime());
        long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, diff.toInteger());

        Date startDate = new Date()
        Date endDate = calendar.getTime();
        boolean[] keyUsage = certificate.getKeyUsage()

        try{
            parentCert.getCertificate().getX509Certificate().checkValidity(endDate)
        }catch(CertificateExpiredException ignored){
            LOG.error("Unable to renew managed certificate with ID {}, the new notAfter date would be after the expiration date of the signer certificate assigned to it! ",certificateId)
            throw new Exception("ASSIGN A SIGNER CERTIFICATE THAT WILL BE VALID AT THE NEW NOTAFTER DATE FOR THE ISSUED CERTIFICATE")
        }catch(CertificateNotYetValidException exception){
            LOG.debug("Exception: ", exception)
            throw new Exception("SIGNER CERTIFICATE NOT YET VALID")
        }


        //KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(certificate.getSigAlgName().toString(), BC_PROVIDER)
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(certificate.getPublicKey().getAlgorithm(), BC_PROVIDER)
        //CAST public key to RSA public to get modulus/keysize
        RSAPublicKey pub = (RSAPublicKey) certificate.getPublicKey()
        keyPairGenerator.initialize(pub.getModulus().bitLength())

        // Generate a new KeyPair and sign it using the Root Cert Private Key
        // by generating a CSR (Certificate Signing Request)
        String issuedSubject = certificate.subjectDN.toString()

        X500Name issuedCertSubject = new X500Name(issuedSubject)
        BigInteger issuedCertSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()))
        KeyPair issuedCertKeyPair = keyPairGenerator.generateKeyPair()

        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(issuedCertSubject, issuedCertKeyPair.getPublic())
        JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder(certificate.getSigAlgName().toString()).setProvider(BC_PROVIDER)

        // Sign the new KeyPair with the root cert Private Key
        ContentSigner csrContentSigner = csrBuilder.build(parentCert.getCertificate().getPrivateKey())
        PKCS10CertificationRequest csr = p10Builder.build(csrContentSigner)
        // Use the Signed KeyPair and CSR to generate an issued Certificate
        // Here serial number is randomly generated. In general, CAs use
        // a sequence to generate Serial number and avoid collisions
        X500Name parentCertSubject = new X500Name(parentCert.getCertificate().getX509Certificate().subjectDN.toString())
        X509v3CertificateBuilder issuedCertBuilder = new X509v3CertificateBuilder(parentCertSubject, issuedCertSerialNum, startDate, endDate, csr.getSubject(), csr.getSubjectPublicKeyInfo())

        JcaX509ExtensionUtils issuedCertExtUtils = new JcaX509ExtensionUtils()

        // Add Extensions
        // Use BasicConstraints to say that this Cert is not a CA
        if(keyUsage[5]){
            issuedCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true))
        }else {
            issuedCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false))
        }

        // Add Issuer cert identifier as Extension
        issuedCertBuilder.addExtension(Extension.authorityKeyIdentifier, false, issuedCertExtUtils.createAuthorityKeyIdentifier(parentCert.getCertificate().getX509Certificate()))
        issuedCertBuilder.addExtension(Extension.subjectKeyIdentifier, false, issuedCertExtUtils.createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo()))

        // Add intended key usage extension if needed
        issuedCertBuilder.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.keyEncipherment))

        // Add DNS name is cert is to used for SSL
        if(certificate.getSubjectAlternativeNames().getAt(2).toString() != null && certificate.getSubjectAlternativeNames().getAt(7).toString() != null ){
            issuedCertBuilder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(new ASN1Encodable[] {
                    new GeneralName(GeneralName.dNSName, getAlternateNames(certificate,2)),
                    new GeneralName(GeneralName.iPAddress, getAlternateNames(certificate,7))
            }))
        }else if(certificate.getSubjectAlternativeNames().getAt(2)!= null){
            issuedCertBuilder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(new ASN1Encodable[] {
                    new GeneralName(GeneralName.dNSName, getAlternateNames(certificate,2)),
            }))
        }else if(certificate.getSubjectAlternativeNames().getAt(7) != null){
            issuedCertBuilder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(new ASN1Encodable[] {
                    new GeneralName(GeneralName.iPAddress, getAlternateNames(certificate,7))
            }))
        }

        X509CertificateHolder issuedCertHolder = issuedCertBuilder.build(csrContentSigner)
        X509Certificate issuedCert  = new JcaX509CertificateConverter().setProvider(BC_PROVIDER).getCertificate(issuedCertHolder)

        // Verify the issued cert signature against the root (issuer) cert
        issuedCert.verify(parentCert.getCertificate().getX509Certificate().getPublicKey(), BC_PROVIDER)
        //writeCertToFileBase64Encoded(issuedCert, "re-issued-cert.cer")
        //exportKeyPairToKeystoreFile(issuedCertKeyPair, issuedCert, "re-issued-cert", "issued-cert.pfx", "PKCS12", "password")

        List<CaCertificate> caCertificates = repository.findByCertificate(certificateToRenew)
        List<StandaloneCertificate> standaloneCertificates = standaloneCertificateRepository.findByCertificate(certificateToRenew)
        List<KeystoreCertificate> keystoreCertificates = keystoreCertificateRepository.findByCertificate(certificateToRenew)
        //TODO IMPLEMENT STANDALONE AND KEYSTORE REPLACEMENT PROPAGATION
        certificateToRenew.setX509Certificate(issuedCert)
        certificateToRenew.setPrivateKey(issuedCertKeyPair.getPrivate())
        certificateRepository.save(certificateToRenew)
        LOG.info("Finished renewal process!")
    }
    //Refactored?
    void assignSignerCaCert(Long signerCertificateId, List<CaCertificateFormGUI>  caCertificateFormGUI){
        CaCertificate signerCertificate = repository.findById(signerCertificateId).get()
        caCertificateFormGUI.forEach(r->{
            CaCertificate caCertificate = repository.findById(r.getId()).get()
            eu.outerheaven.certmanager.controller.entity.Certificate cert = caCertificate.getCertificate()
            cert.setSignerCertificateId(signerCertificate.getCertificate().getId())
            certificateRepository.save(cert)
            //repository.save(caCertificate)
        })
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
    //Refactored
    void exportKeyPairToKeystoreFile(KeyPair keyPair, Certificate certificate, String alias, String fileName, String storeType, String storePass) throws Exception {
        KeyStore sslKeyStore = KeyStore.getInstance(storeType, BC_PROVIDER)
        sslKeyStore.load(null, null)
        LOG.info("Saved private key is: " + keyPair.getPrivate())
        sslKeyStore.setKeyEntry(alias, keyPair.getPrivate(),null, new Certificate[]{certificate})
        FileOutputStream keyStoreOs = new FileOutputStream(fileName)
        sslKeyStore.store(keyStoreOs, storePass.toCharArray())
        getKeyPairFromKeystore(alias,fileName,storeType,storePass)

    }
    //Refactored
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
    //Refactored
    void getKeyPairFromKeystore(String alias, String fileName, String storeType, String storePass){
        KeyStore sslKeystore = KeyStore.getInstance(storeType, BC_PROVIDER)
        InputStream inputstream = new FileInputStream(fileName)
        sslKeystore.load(inputstream, storePass.toCharArray())
        Key key = sslKeystore.getKey(alias)
        LOG.info("Read private key is: " + key)
    }
    //Refactored
    CaCertificateFormGUI toFormGUI(CaCertificate caCertificate){
        String level
        if(caCertificate.getCertificate().x509Certificate.issuerDN == caCertificate.getCertificate().getX509Certificate().subjectDN){
            level = "ROOT"
        }else{
            level="INTERMEDIATE"
        }
        CaCertificateFormGUI caCertificateFormGUI = new CaCertificateFormGUI(
                id: caCertificate.id,
                alias: caCertificate.alias,
                managed: caCertificate.getCertificate().managed,
                status: certStatus(caCertificate.getCertificate().getX509Certificate().notBefore,caCertificate.getCertificate().getX509Certificate().notAfter),
                level: level,
                subject: caCertificate.getCertificate().getX509Certificate().subjectDN,
                issuer: caCertificate.getCertificate().getX509Certificate().issuerDN,
                validFrom: caCertificate.getCertificate().getX509Certificate().notBefore,
                validTo: caCertificate.getCertificate().getX509Certificate().notAfter,
                serial: caCertificate.getCertificate().getX509Certificate().serialNumber
        )
        return caCertificateFormGUI
    }
    //Refactored
    List<CaCertificateFormGUI> toFormGUI(List<CaCertificate> caCertificates){
        List<CaCertificateFormGUI> certificateFormGUIS = new ArrayList<>()
        caCertificates.forEach(r->certificateFormGUIS.add(toFormGUI(r)))
        return certificateFormGUIS
    }
    //Refactored
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
    //Refactored
    String getCleanCertName(Long certificateId){
        CaCertificate caCertificate = repository.findById(certificateId).get()
        String filename =""
        if(caCertificate.getAlias() == null){
            filename ="certificate.cer"
        }else {
            filename =caCertificate.getAlias() + ".cer"
        }
        filename = filename.replaceAll("[^\\dA-Za-z. ]", "").replaceAll("\\s+", "_")

        return filename
    }
    //Refactored
    Resource exportAsPem(Long certificateId, String filename){
        CertificateLoader certificateLoader = new CertificateLoader()
        String folderName = certificateLoader.generateRandomName()
        Path path = Paths.get(folderName)
        while (Files.exists(path)){
            folderName = certificateLoader.generateRandomName()
            path=Paths.get(folderName)
        }
        try{
            new File(path.toString()).mkdirs()
            CaCertificate caCertificate = repository.findById(certificateId).get()
            certificateLoader.writeCertToFileBase64Encoded(caCertificate.getCertificate().getX509Certificate(),folderName + "/" + filename)
            if(caCertificate.getCertificate().privateKey != null){
                certificateLoader.writeKeyToFileBase64Encoded(caCertificate.getCertificate().getPrivateKey(), folderName + "/" + filename)
            }
            String fullPath =  "./" + folderName + "/" + filename
            LOG.info("Path to the file is " + fullPath)
            path = Paths.get(folderName + "/" + filename)
            File file = new File(folderName + "/" + filename)
            byte[] fileContent = Files.readAllBytes(file.toPath())

            //Resource resource = new UrlResource(path.toUri())
            Resource resource = new ByteArrayResource(fileContent)
            FileUtils.deleteDirectory(path.getParent().toFile())
            return resource
        }catch(Exception exception){
            LOG.error("Failed exporting certificate:" + exception)
        }
    }
    //Refactored?
    void importCertificate(CertificateImportDto certificateImportDto){
        List<CaCertificate> certificates = new ArrayList<>()
        CertificateLoader certificateLoader = new CertificateLoader()
        if(certificateImportDto.getImportFormat() == "PEM"){
            List<eu.outerheaven.certmanager.controller.entity.Certificate> tmpcertificates = certificateLoader.decodeImportPem(certificateImportDto.getBase64File(), certificateImportDto.getFilename())
            tmpcertificates.forEach(r->{
                CaCertificate tmp = new CaCertificate(
                        alias: "IMPORTED_CHANGE_ME",
                        certificate: r
                )
                certificates.add(tmp)
            })
        }else {
            List<KeystoreCertificate> tmpcertificates = certificateLoader.decodeImportPCKS12(certificateImportDto.getBase64File(), certificateImportDto.getPassword())
            tmpcertificates.forEach(r->{
                CaCertificate tmp = new CaCertificate(
                        alias: "IMPORTED_CHANGE_ME",
                        certificate: r.certificate
                )
                certificates.add(tmp)
            })
        }
        certificates.forEach(r->{repository.save(r)})

    }
    //TODO this shit, first fix cert loader
    void replaceCertificate(CertificateImportDto certificateImportDto, Long certId){
        List<CaCertificate> caCertificates = new ArrayList<>()
        CertificateLoader certificateLoader = new CertificateLoader()
        caCertificates = certToCaCert(certificateLoader.decodeImportPem(certificateImportDto.getBase64File(), certificateImportDto.getFilename()))
        if(caCertificates.size()>1) throw new Exception("Cannot replace one certificate with multiple ones!")
        CaCertificate caCertificate = repository.findById(certId).get()
        eu.outerheaven.certmanager.controller.entity.Certificate certificate = caCertificate.getCertificate()
        certificate.setX509Certificate(caCertificates.get(0).x509Certificate)
        caCertificate.setX509Certificate(caCertificates.get(0).x509Certificate)
        caCertificate.setPrivateKey(caCertificates.get(0).privateKey)
        repository.save(caCertificate)
    }

    //Goes over
    List<CaCertificate> purgeCertDuplicates(List<CaCertificate> certificates){
        List<CaCertificate> purgedList = new ArrayList<>()
        certificates.forEach(r-> {
            eu.outerheaven.certmanager.controller.entity.Certificate cert = certificateRepository.findByX509Certificate(r.getCertificate().x509Certificate)
            if(cert == null){
                purgedList.add(r)
            }else{
                CaCertificate newcert = new CaCertificate(
                        alias: r.alias,
                        certificate: cert,
                )
                purgedList.add(newcert)
            }
        })

        return purgedList
    }

    @Transactional
    void scheduledCheck(){
        if(environment.getProperty("controller.expiration.check").toBoolean()){
            LOG.info("Starting scheduled job: expiration check for certificates in CA Vault");
            Date date = new Date()
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, environment.getProperty("controller.expiration.check.warn.period").toInteger());
            Date currentDatePlus= calendar.getTime();
            List<CaCertificate> caCertificates = repository.findAll() as List<CaCertificate>
            List<CaCertificate> expiredCertificates = new ArrayList<>()
            List<CaCertificate> soonToExpireCertificates = new ArrayList<>()

            caCertificates.forEach(r->{

                boolean alreadyExpired=false
                boolean renewed = false
                try{
                    r.getCertificate().getX509Certificate().checkValidity(date)
                }catch(CertificateExpiredException exception){
                    if(r.getCertificate().managed){
                        LOG.info("Renewing certificate in CA vault with alias {} and ID {}", r.alias,r.id)
                        renew(r.getCertificate())
                        renewed = true
                    }else if(!renewed){
                        expiredCertificates.add(r)
                        alreadyExpired=true
                        LOG.warn("Certificate with alias {} has already expired!", r.getAlias())
                        LOG.debug("Exception: ", exception)
                    }
                }catch(CertificateNotYetValidException exception){
                    LOG.debug("Exception: ", exception)
                }
                if(!alreadyExpired || !renewed){
                    try{
                        r.getCertificate().getX509Certificate().checkValidity(currentDatePlus)
                    }catch(CertificateExpiredException exception){
                        soonToExpireCertificates.add(r)
                        LOG.warn("Certificate with alias {} is within expiration warning period!", r.getAlias())
                        LOG.debug("Exception: ", exception)
                    }catch(CertificateNotYetValidException exception){
                        LOG.info("Certificate with alias {} is not yet valid!", r.getAlias())
                        LOG.debug("Exception: ", exception)
                    }
                }

            })
            if(soonToExpireCertificates.size()>0 || expiredCertificates.size()>0 && environment.getProperty("controller.mail.expiration.alert").toBoolean()){
                mailService.sendKeystoreCaCertificateExpirationAlert(expiredCertificates,soonToExpireCertificates)
            }

            LOG.info("Ended scheduled job: expiration check for certificates in CA Vault");
        }
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
}
