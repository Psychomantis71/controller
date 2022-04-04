package eu.outerheaven.certmanager.controller.util

import com.ibm.security.cmskeystore.CMSProvider
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream
import eu.outerheaven.certmanager.controller.entity.Keystore
import eu.outerheaven.certmanager.controller.entity.KeystoreCertificate
import org.bouncycastle.asn1.cms.KEKIdentifier
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import eu.outerheaven.certmanager.controller.entity.Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.security.cert.CertificateEncodingException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.security.*
import java.security.cert.*
import java.security.spec.PKCS8EncodedKeySpec
import java.util.concurrent.ThreadLocalRandom
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Class that contains method to load certificates in {@link java.security.cert.X509Certificate}.
 */
class CertificateLoader {

    private static final Logger LOG = LoggerFactory.getLogger(CertificateLoader.class)

    /**
     * Method for getting certificate file.
     *
     * @param uri certificate web url
     * @return loaded certificate
     * @throws CertificateException error
     * @throws IOException          error
     */
    static X509Certificate loadFileCertificate(String uri) throws CertificateException, IOException {
        if (uri.toLowerCase().endsWith("pem")) {
            InputStream res = Files.newInputStream(Paths.get(uri))
            Reader fRd = new BufferedReader(new InputStreamReader(res))
            PEMParser pemParser = new PEMParser(fRd)
            return (X509Certificate) pemParser.readObject()
        }
        return (X509Certificate) CertificateFactory
                .getInstance("X509")
                .generateCertificate(
                        new ByteArrayInputStream(Files.readAllBytes(Paths.get(uri)))
                )
    }

    /**
     * Method for getting certificate and its trust chain.
     *
     * @param uri certificate web url
     * @return list of certificates
     * @throws KeyManagementException       error
     * @throws NoSuchAlgorithmException     error
     * @throws IOException                  error
     * @throws CertificateException         error
     * @throws CertificateEncodingException error
     */
    //UPDATED
    static List<Certificate> loadWebCertificates(String uri) throws IOException, NoSuchAlgorithmException,
            CertificateException, CertificateEncodingException, KeyManagementException, URISyntaxException {
        if (!uri.startsWith("https")) {
            int index = uri.indexOf("//")
            if (index != -1)
                uri = uri.substring(index + 2)
            uri = "https://" + uri
        }
        URI certURI = new URI(uri)
        return loadCertificatesFromHost(certURI.getHost(), certURI.getPort() != -1 ? certURI.getPort() : 443)
    }

    /**
     * Method for loading certificate chain from some host and port.
     *
     * @param host host
     * @param port port
     * @return list of certificates
     * @throws KeyManagementException       error
     * @throws NoSuchAlgorithmException     error
     * @throws IOException                  error
     * @throws CertificateException         error
     * @throws CertificateEncodingException error
     */
    //UPDATED
    List<Certificate> loadCertificatesFromHost(String host, int port) throws KeyManagementException,
            NoSuchAlgorithmException, IOException, CertificateException, CertificateEncodingException {
        SSLSocket socket = null
        try {
            if (port == -1) {
                throw new RuntimeException("Port could not be found from URI")
            }
            List<Certificate> certificates = new ArrayList<>()
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, null, null);
            SSLSocketFactory factory = context.getSocketFactory()
            socket = (SSLSocket) factory.createSocket(host, port)
            socket.startHandshake()
            SSLSession session = socket.getSession()

            for (javax.security.cert.X509Certificate certificate : session.getPeerCertificateChain()) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509")
                ByteArrayInputStream bais = new ByteArrayInputStream(certificate.getEncoded())
                Certificate custom_certificate = new Certificate()
                custom_certificate.setX509Certificate(cf.generateCertificate(bais) as X509Certificate)
                custom_certificate.setManaged(false)
                certificates.add(custom_certificate)
                //certificates.add((X509Certificate) cf.generateCertificate(bais))
            }
            return certificates
        } finally {
            if (socket != null) {
                socket.close()
            }
        }
    }


    /**
     * Method for loading certificates from keystore,
     *
     * @param uri uri to keystore
     * @return list of certificates
     * @throws KeyStoreException        error
     * @throws IOException              error
     * @throws CertificateException     error
     * @throws NoSuchAlgorithmException error
     */
    //UPDATED
    static List<Certificate> loadCertificatesFromKeystore(String uri, String password, Keystore tKeystore) throws KeyStoreException,
            IOException, CertificateException, NoSuchAlgorithmException {
        String[] types = new String[]{"JKS", "JCEKS", "PKCS12", "IBMCMSKS",/*BC types*/ "BKS", "PKCS12", "UBER"}
        boolean read = false
        //List<X509Certificate> certificates = new ArrayList<>()
        List<Certificate> certificates = new ArrayList<>()
        Security.addProvider(new BouncyCastleProvider())
        Security.addProvider(new CMSProvider())
        for (int i = 0; i < types.length; ++i) {
            try {
                KeyStore keystore
                if (i >= 4) { //BC
                    keystore = KeyStore.getInstance(types[i], "BC")
                } else { //SUN AND IBM
                    keystore = KeyStore.getInstance(types[i])
                }

                keystore.load(new FileInputStream(uri), password.toCharArray())
                LOG.debug("Reading aliases from keystore")
                Enumeration<String> aliases = keystore.aliases()
                while (aliases.hasMoreElements()) {
                    Certificate certificate = new Certificate()
                    String alias = aliases.nextElement()
                    certificate.setAlias(alias)
                    certificate.setX509Certificate(keystore.getCertificate(alias) as X509Certificate)
                    certificate.setManaged(false)
                    certificates.add(certificate)
                    certificate.setKeystoreId(tKeystore.getId())
                    //certificates.add((X509Certificate) keystore.getCertificate(alias))
                    LOG.info("Read certificate with alias: " + alias)
                }
                read = true
                break
            } catch (Exception e) {
                LOG.error("Reading keystore with type " + types[i] + " : " + e.toString())
            }
        }
        //needed?
        // Security.removeProvider("BC")
        if (!read) {
            throw new RuntimeException("Could not read keystore: " + uri)
        }
        return certificates
    }


    //UPDATED (NOTE: DOES NOT APPLY ALIAS)
    static List<Certificate> loadCertificatesFromCacerts(String uri, String password) throws KeyStoreException,
            IOException, CertificateException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        //List<X509Certificate> certificates = new ArrayList<>()
        List<Certificate> certificates = new ArrayList<>()
        // Load the JDK's cacerts keystore file
        FileInputStream is = new FileInputStream(uri)
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType())
        keystore.load(is, password.toCharArray())
        // This class retrieves the most-trusted CAs from the keystore
        PKIXParameters params = new PKIXParameters(keystore)

        // Get the set of trust anchors, which contain the most-trusted CA certificates
        for (TrustAnchor ta : params.getTrustAnchors()) {
            // Get certificate
            X509Certificate cert = ta.getTrustedCert()
            Certificate certificate = new Certificate()
            certificate.setX509Certificate(cert)
            certificate.setManaged(false)
            certificates.add(certificate)
        }

        return certificates
    }

    //TODO PEM
    static List<X509Certificate> getPublicCertFromPEM(String path) throws IOException, CertificateException {
        CertificateFactory fact = CertificateFactory.getInstance("X.509")
        // TODO problem ako se u fileu nađe private key ...
        //Ovo čita samo certove
        try(FileInputStream is = new FileInputStream (path)){
            final Collection<X509Certificate> certs = (Collection<X509Certificate>) fact.generateCertificates(is)
            return new ArrayList<>(certs)
        }
    }

    //USED below

    String encodeX509(X509Certificate x509Certificate){
        try{
            ByteArrayOutputStream binaryOutput = new ByteArrayOutputStream()
            ObjectOutputStream objectStream = new ObjectOutputStream(binaryOutput)
            objectStream.writeObject(x509Certificate)
            objectStream.close()
            binaryOutput.close()
            return Base64.getUrlEncoder().encodeToString(binaryOutput.toByteArray())
        }catch (Exception exception){
            LOG.error("Could not encode X509Certificate to base64 with error: " + exception)
        }
    }

    X509Certificate decodeX509(String input){
        try{
            byte [] data = Base64.getUrlDecoder().decode(input)
            ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(data))
            X509Certificate x509Certificate = objectInputStream.readObject() as X509Certificate
            objectInputStream.close()
            return x509Certificate
        }catch(Exception exception){
            LOG.error("Could not decode  base64 to X509Certificate with error: " + exception)
        }
    }

    List<String> encodeX509List(List<X509Certificate> decodedCerts){
        List<String> encodedCerts = new ArrayList<>()
        decodedCerts.forEach(r->encodedCerts.add(encodeX509(r)))
        return encodedCerts
    }

    List<X509Certificate> decodeX509List(List<String> encodedCerts){
        List<X509Certificate> decodedCerts = new ArrayList<>()
        encodedCerts.forEach(r->decodedCerts.add(decodeX509(r)))
        return encodedCerts
    }
    String encodeKey(PrivateKey key){
        try{
            ByteArrayOutputStream binaryOutput = new ByteArrayOutputStream()
            ObjectOutputStream objectStream = new ObjectOutputStream(binaryOutput)
            objectStream.writeObject(key)
            objectStream.close()
            binaryOutput.close()
            return Base64.getUrlEncoder().encodeToString(binaryOutput.toByteArray())
        }catch (Exception exception){
            LOG.error("Could not encode Key to base64 with error: " + exception)
        }
    }

    PrivateKey decodeKey(String input){
        try{
            byte [] data = Base64.getUrlDecoder().decode(input)
            ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(data))
            PrivateKey key = objectInputStream.readObject() as PrivateKey
            objectInputStream.close()
            return key
        }catch(Exception exception){
            LOG.error("Could not decode base64 to key with error: " + exception)
        }
    }

    void writeCertToFileBase64Encoded(X509Certificate certificate, String fileName) throws Exception {
        FileOutputStream certificateOut = new FileOutputStream(fileName)
        String certData = new String(Base64.getEncoder().encode(certificate.getEncoded()))
        certData = certData.replaceAll("(.{67})", "\$1\n")
        certificateOut.write("-----BEGIN CERTIFICATE-----\n".getBytes())
        certificateOut.write(certData.getBytes())
        certificateOut.write("\n-----END CERTIFICATE-----".getBytes())
        certificateOut.close()
    }

    void writeKeyToFileBase64Encoded(PrivateKey privateKey, String fileName) throws Exception {
        FileOutputStream certificateOut = new FileOutputStream(new File(fileName),true)
        String keyData = new String(Base64.getEncoder().encode(privateKey.getEncoded()))
        keyData = keyData.replaceAll("(.{67})", "\$1\n")
        certificateOut.write("\n-----BEGIN PRIVATE KEY-----\n".getBytes())
        //certificateOut.write(Base64.encode(certificate.getEncoded()))
        certificateOut.write(keyData.getBytes())
        certificateOut.write("\n-----END PRIVATE KEY-----".getBytes())
        certificateOut.close()
    }

    String generateRandomName(){
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10
        Random random = new Random()

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString()

        return generatedString
    }

    String generateRandomAlphanumeric(){
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 20
        Random random = new Random()

        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();


        return generatedString
    }


    Boolean doesKeyPairMatch(PrivateKey privateKey, PublicKey publicKey){
        byte[] challenge = new byte[10000];
        ThreadLocalRandom.current().nextBytes(challenge)
        Signature sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(privateKey)
        sig.update(challenge)
        byte[] signature = sig.sign()
        sig.initVerify(publicKey);
        sig.update(challenge)
        boolean keyPairMatches = false
        try{
            keyPairMatches = sig.verify(signature)
        }catch(Exception exception){
            LOG.debug("Keypair does not match")
        }
        return keyPairMatches
    }

    List<Certificate> matchAndConstruct(List<PrivateKey> privateKeys, List<X509Certificate> x509Certificates, String filename){
        List<Certificate> certificates = new ArrayList<>()
        for(int i=0; i<x509Certificates.size();i++){
            for(int n=0; n<privateKeys.size();n++){
                if(doesKeyPairMatch(privateKeys.get(n),x509Certificates.get(i).getPublicKey())){
                    LOG.info("Keypair match found!")
                    Certificate certificate = new Certificate(
                            privateKey: privateKeys.get(n),
                            x509Certificate: x509Certificates.get(i),
                            managed: false,
                    )
                    certificates.add(certificate)
                    privateKeys.remove(n)
                }
            }
            if(certificates.size() < i+1){
                Certificate certificate = new Certificate(
                        x509Certificate: x509Certificates.get(i),
                        managed: false,
                )
                certificates.add(certificate)
            }
        }
        if(privateKeys.size()>0){
            LOG.error("WHAT THE FUCK MAN WHY IS THERE A PRIVATE KEY WITHOUT MATCH?!")
            throw new Exception("You can not import a private key without a matching public certificate");
        }
        return certificates
    }

    List<Certificate> decodeImportPem(String input, String filename){
        try{
            filename= filename.substring(0, filename.lastIndexOf('.'))
            byte [] data = Base64.getUrlDecoder().decode(input.getBytes(StandardCharsets.UTF_8))
            String decodedData = new String(data,StandardCharsets.UTF_8)

            List<X509Certificate> certificates = new ArrayList<>()
            List<PrivateKey> privateKeys = new ArrayList<>()

            String pattern1 = "-----BEGIN CERTIFICATE-----";
            String pattern2 = "-----END CERTIFICATE-----";
            String pattern3 = "-----BEGIN PRIVATE KEY-----";
            String pattern4 = "-----END PRIVATE KEY-----";
            Pattern p = Pattern.compile(Pattern.quote(pattern1) + "(?s)(.*?)" + Pattern.quote(pattern2));
            Matcher m = p.matcher(decodedData);
            while (m.find()) {
                String certdatatmp = m.group(1)
                certdatatmp = certdatatmp.replaceAll("\\s+","")
                byte [] certdata = Base64.getDecoder().decode(certdatatmp)
                ByteArrayInputStream inputStream  =  new ByteArrayInputStream(certdata)
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                X509Certificate x509Certificate = (X509Certificate)certFactory.generateCertificate(inputStream)
                certificates.add(x509Certificate)
                x509Certificate.getPublicKey()
                LOG.info("Public certificate found in pem import")
            }

            p = Pattern.compile(Pattern.quote(pattern3) + "(?s)(.*?)" + Pattern.quote(pattern4));
            m = p.matcher(decodedData);
            while (m.find()) {
                String keydatatmp = m.group(1)
                keydatatmp = keydatatmp.replaceAll("\\s+","")

                byte [] pkcs8EncodedBytes = Base64.getDecoder().decode(keydatatmp)
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PrivateKey privateKey = kf.generatePrivate(keySpec)
                privateKeys.add(privateKey)
                LOG.info("Private key found in pem import")
            }
            List<Certificate> processedCertificates = new ArrayList<>()
            if(privateKeys.size()>0){
                LOG.info("Private key/s have been found in import, trying to find matching public key in import")
                processedCertificates = matchAndConstruct(privateKeys, certificates, filename)
            }else{
                for(int i=0;i<certificates.size();i++){
                    Certificate certificate = new Certificate(
                            x509Certificate: certificates.get(i),
                            managed: false,
                    )
                    processedCertificates.add(certificate)
                }
            }

            LOG.info("Found {} certificates in the import",processedCertificates.size())
            return processedCertificates
        }catch(Exception exception){
            LOG.error("Well fuck, something failed with the import: " + exception)
        }
    }

    List<KeystoreCertificate> decodeImportPCKS12(String input, String password) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException{
        byte [] data = Base64.getDecoder().decode(input.getBytes(StandardCharsets.UTF_8))
        String[] types = new String[]{"JKS", "JCEKS", "PKCS12", "IBMCMSKS",/*BC types*/ "BKS", "PKCS12", "UBER"}
        boolean read = false
        List<KeystoreCertificate> keystoreCertificates = new ArrayList<>()
        Security.addProvider(new BouncyCastleProvider())
        Security.addProvider(new CMSProvider())
        for (int i = 0; i < types.length; ++i) {
            try {
                KeyStore keystore
                if (i >= 4) { //BC
                    keystore = KeyStore.getInstance(types[i], "BC")
                } else { //SUN AND IBM
                    keystore = KeyStore.getInstance(types[i])
                }

                keystore.load(new ByteArrayInputStream(data), password.toCharArray())
                LOG.debug("Reading aliases from keystore")
                Enumeration<String> aliases = keystore.aliases()
                while (aliases.hasMoreElements()) {
                    KeystoreCertificate keystoreCertificate = new KeystoreCertificate()
                    Certificate certificate = new Certificate()
                    String alias = aliases.nextElement()
                    if(keystore.getKey(alias,password.toCharArray()) != null){
                        certificate.setPrivateKey(keystore.getKey(alias,password.toCharArray()) as PrivateKey)
                        keystoreCertificate.setKeypair(true)
                        LOG.info("Certificate with alias {} has a private key attached to it!",alias)
                    }
                    keystoreCertificate.setAlias(alias)
                    certificate.setX509Certificate(keystore.getCertificate(alias) as X509Certificate)
                    certificate.setManaged(false)
                    keystoreCertificate.setCertificate(certificate)
                    keystoreCertificates.add(keystoreCertificate)
                    //certificates.add((X509Certificate) keystore.getCertificate(alias))
                    LOG.debug("Read certificate with alias: " + alias)
                }
                read = true
                break
            } catch (Exception e) {
                LOG.error("Reading keystore with type " + types[i] + " : " + e.toString())
            }

        }
        if (!read) {
            throw new RuntimeException("Could not read imported keystore: ")
        }
        return keystoreCertificates
    }

}
