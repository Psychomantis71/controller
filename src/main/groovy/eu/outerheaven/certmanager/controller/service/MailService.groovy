package eu.outerheaven.certmanager.controller.service

import eu.outerheaven.certmanager.controller.entity.Certificate
import eu.outerheaven.certmanager.controller.entity.Instance
import eu.outerheaven.certmanager.controller.entity.Keystore
import eu.outerheaven.certmanager.controller.entity.User
import org.springframework.beans.factory.annotation.Autowired
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

        helper.setFrom("obama@yourass.com")
        helper.setTo(setTo);

        helper.setSubject("OSCM Alert");
        String text
        text = "<h1>OSCM has detected modifications on a keystore</h1>"
        text = text + "<p> Affected instance name ${instance.getName()} and IP ${instance.getIp()} </p>"
        text = text + "<p> Affected keystore name ${keystore.getLocation()} and ID ${keystore.getId()} </p>"
        // default = text/plain
        //helper.setText("Check attachment for image!");

        // true = text/html

        if(addedCertificates.size()>0){
            text = text + "<h2>Added certificates: </h2>"
            addedCertificates.forEach(r->{
                text = text + "<p>Certificate with ID ${r.getId()} alias ${r.getAlias()}, subject ${r.getX509Certificate().getSubjectDN()} has been added</p>"
            })
        }

        if(modifiedCertificates.size()>0){
            text = text + "<h2>Modified certificates: </h2>"
            modifiedCertificates.forEach(r->{
                text = text + "<p>Certificate with ID ${r.getId()} alias ${r.getAlias()}, subject ${r.getX509Certificate().getSubjectDN()} has been modified</p>"
            })
        }

        if(removedCertificates.size()>0){
            text = text + "<h2>Removed certificates: </h2>"
            removedCertificates.forEach(r->{
                text = text + "<p>Certificate with ID ${r.getId()} alias ${r.getAlias()}, subject ${r.getX509Certificate().getSubjectDN()} has been removed</p>"
            })
        }
        // hard coded a file path
        //FileSystemResource file = new FileSystemResource(new File("path/android.png"));
        helper.setText(text,true)

        javaMailSender.send(msg);

    }


}
