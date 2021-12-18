package eu.outerheaven.certmanager.controller

import eu.outerheaven.certmanager.controller.entity.User
import eu.outerheaven.certmanager.controller.entity.UserRole
import eu.outerheaven.certmanager.controller.repository.UserRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.PropertySource
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

import javax.annotation.PostConstruct
import java.util.stream.Collectors
import java.util.stream.Stream

@ConfigurationPropertiesScan
@SpringBootApplication
@EnableScheduling
class ControllerApplication {
    private static final Logger LOG = LoggerFactory.getLogger(ControllerApplication)
    private static BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder()

    @Autowired
    private UserRepository repository;

    @PostConstruct
    void initUsers() {
        List<User> users = Stream.of(
                new User(98,"psychomantis", passwordEncoder.encode("password"), "adrian.perkovic71@gmail.com", UserRole.ADMIN),
                new User(100,"admin", passwordEncoder.encode("password"), "adrian.perkovic71@gmail.com", UserRole.ADMIN),
                new User(102, "agent_user", passwordEncoder.encode("kuracnabiciklu"), "", UserRole.AGENT),
                new User(104,"recipient1", passwordEncoder.encode("password"), "adrian.perkovic71@gmail.com", UserRole.RECIPIENT),
                new User(106,"user1", passwordEncoder.encode("password"), "adrian.perkovic71@gmail.com", UserRole.USER),
                new User(108,"recipient2", passwordEncoder.encode("password"), "adrian.perkovic71@gmail.com", UserRole.RECIPIENT),
                new User(110,"user2", passwordEncoder.encode("password"), "aperkovic@croz.net", UserRole.USER),
        ).collect(Collectors.toList());
        repository.saveAll(users);


        LOG.debug("Found users on startup: " + repository.count() )
    }

    static void main(String[] args) {
        SpringApplication.run(ControllerApplication, args)
    }

}
