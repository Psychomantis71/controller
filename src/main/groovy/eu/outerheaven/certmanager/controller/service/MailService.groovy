package eu.outerheaven.certmanager.controller.service

import eu.outerheaven.certmanager.controller.entity.CaCertificate
import eu.outerheaven.certmanager.controller.entity.Certificate
import eu.outerheaven.certmanager.controller.entity.Instance
import eu.outerheaven.certmanager.controller.entity.Keystore
import eu.outerheaven.certmanager.controller.entity.User
import eu.outerheaven.certmanager.controller.entity.UserRole
import eu.outerheaven.certmanager.controller.repository.KeystoreRepository
import eu.outerheaven.certmanager.controller.repository.UserRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

import javax.mail.MessagingException
import javax.mail.internet.MimeMessage

@Service
class MailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private KeystoreRepository keystoreRepository

    @Autowired
    private UserRepository userRepository

    private static final Logger LOG = LoggerFactory.getLogger(MailService.class)

    /*
    @Bean
    JavaMailSender getJavaMailSender() {
        try (InputStream input = new FileInputStream("controller.config")) {
            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            // get the property value and print it out
            user = prop.getProperty("controller.user")
            password = prop.getProperty("controller.password")

            input.close()
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);

        mailSender.setUsername("my.gmail@gmail.com");
        mailSender.setPassword("password");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }
    */
    void sendKeystoreAlert(List<Certificate> modifiedCertificates, List<Certificate> addedCertificates, List<Certificate> removedCertificates, Instance instance, Keystore keystore) throws MessagingException, IOException {

        MimeMessage msg = javaMailSender.createMimeMessage();

        // true = multipart message
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);

        List<String> alertEmails = new ArrayList<>()
        List<User> users = instance.getAssignedUsers()

        users.forEach(r->{
            alertEmails.add(r.getEmail())
        })

        String[] setTo = alertEmails.toArray(new String[0])

        helper.setFrom("oscm-controller@croz.net")
        helper.setTo(setTo);

        helper.setSubject("OSCM Modification Alert");
        String text
        text = "<h1>OSCM has detected modifications on a keystore</h1>"
        text = text + "<p> Affected instance name ${instance.getName()} and IP ${instance.getIp()} </p>"
        text = text + "<p> Affected keystore name ${keystore.getLocation()} and ID ${keystore.getId()} </p>"
        // default = text/plain
        //helper.setText("Check attachment for image!");

        // true = text/html

        if(addedCertificates.size()>0){
            text = text + "<h2 style=\"color:green;\">Added certificates: </h2>"
            addedCertificates.forEach(r->{
                text = text + "<p>Certificate with ID ${r.getId()} alias ${r.getAlias()}, subject ${r.getX509Certificate().getSubjectDN()} has been added</p>"
            })
        }

        if(modifiedCertificates.size()>0){
            text = text + "<h2 style=\"color:blue;\">Modified certificates: </h2>"
            modifiedCertificates.forEach(r->{
                text = text + "<p>Certificate with ID ${r.getId()} alias ${r.getAlias()}, subject ${r.getX509Certificate().getSubjectDN()} has been modified</p>"
            })
        }

        if(removedCertificates.size()>0){
            text = text + "<h2 style=\"color:red;\">Removed certificates: </h2>"
            removedCertificates.forEach(r->{
                text = text + "<p>Certificate with ID ${r.getId()} alias ${r.getAlias()}, subject ${r.getX509Certificate().getSubjectDN()} has been removed</p>"
            })
        }
        // hard coded a file path
        //FileSystemResource file = new FileSystemResource(new File("path/android.png"));
        helper.setText(text,true)

        javaMailSender.send(msg);
        LOG.info("Modification alert email has been sent")
    }

    void sendKeystoreCertificateExpirationAlert(List<Certificate> expiredCertificates, List<Certificate> soonToExpireCertificates, Instance instance) throws MessagingException, IOException{


        MimeMessage msg = javaMailSender.createMimeMessage();

        // true = multipart message
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);

        List<String> alertEmails = new ArrayList<>()
        List<User> users = instance.getAssignedUsers()

        users.forEach(r->{
            alertEmails.add(r.getEmail())
        })

        String[] setTo = alertEmails.toArray(new String[0])

        helper.setFrom("oscm-controller@croz.net")
        helper.setTo(setTo);

        helper.setSubject("OSCM Expiration Alert");
        String text
        text = "<h1>OSCM has detected certificates that have either expired or are within the expiration warning period</h1>"
        text = text + "<p> Affected instance name ${instance.getName()} and IP ${instance.getIp()} </p>"
        if(expiredCertificates.size()>0){
            text = text + "<h2 style=\"color:red;\">Already expired certificates: </h2>"
            expiredCertificates.forEach(r->{
                Keystore keystore = keystoreRepository.findById(r.getKeystoreId()).get()
                text = text + "<p>Certificate with ID ${r.getId()} alias ${r.getAlias()}, subject ${r.getX509Certificate().getSubjectDN()} has already expired on ${r.getX509Certificate().getNotAfter()}, located under keystore ID ${keystore.getId()} and path ${keystore.getLocation()} </p>"
            })
        }
        if(soonToExpireCertificates.size()>0){
            text = text + "<h2 style=\"color:red;\">Certificates within expiration warning period: </h2>"
            soonToExpireCertificates.forEach(r->{
                Keystore keystore = keystoreRepository.findById(r.getKeystoreId()).get()
                text = text + "<p>Certificate with ID ${r.getId()} alias ${r.getAlias()}, subject ${r.getX509Certificate().getSubjectDN()} will expire on ${r.getX509Certificate().getNotAfter()}, located under keystore ID ${keystore.getId()} and path ${keystore.getLocation()} </p>"
            })
        }


        helper.setText(text,true)

        javaMailSender.send(msg);
        LOG.info("Expiration alert email has been sent")

    }

    void sendKeystoreCaCertificateExpirationAlert(List<CaCertificate> expiredCertificates, List<CaCertificate> soonToExpireCertificates){

        MimeMessage msg = javaMailSender.createMimeMessage();

        // true = multipart message
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);

        List<String> alertEmails = new ArrayList<>()
        List<User> users = userRepository.findByUserRole(UserRole.ADMIN)

        users.forEach(r->{
            alertEmails.add(r.getEmail())
        })

        String[] setTo = alertEmails.toArray(new String[0])

        helper.setFrom("oscm-controller@croz.net")
        helper.setTo(setTo);

        helper.setSubject("OSCM Expiration Alert");
        String text
        text = "<h1>OSCM has detected certificates that have either expired or are within the expiration warning period</h1>"
        text = text + "<p> Affected certificates are within the CA Vault </p>"
        if(expiredCertificates.size()>0){
            text = text + "<h2 style=\"color:red;\">Already expired certificates: </h2>"
            expiredCertificates.forEach(r->{
                text = text + "<p>Certificate with ID ${r.getId()} alias ${r.getAlias()}, subject ${r.getX509Certificate().getSubjectDN()} has already expired on ${r.getX509Certificate().getNotAfter()}</p>"
            })
        }
        if(soonToExpireCertificates.size()>0){
            text = text + "<h2 style=\"color:red;\">Certificates within expiration warning period: </h2>"
            soonToExpireCertificates.forEach(r->{
                text = text + "<p>Certificate with ID ${r.getId()} alias ${r.getAlias()}, subject ${r.getX509Certificate().getSubjectDN()} will expire on ${r.getX509Certificate().getNotAfter()}</p>"
            })
        }


        helper.setText(text,true)

        javaMailSender.send(msg);
        LOG.info("Expiration alert email has been sent")

    }

}
