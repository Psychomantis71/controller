package eu.outerheaven.certmanager.controller

import eu.outerheaven.certmanager.controller.entity.User
import eu.outerheaven.certmanager.controller.entity.UserRole
import eu.outerheaven.certmanager.controller.repository.UserRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

import javax.annotation.PostConstruct
import java.util.stream.Collectors
import java.util.stream.Stream

@ConfigurationPropertiesScan
@SpringBootApplication
class ControllerApplication {
    private static final Logger LOG = LoggerFactory.getLogger(ControllerApplication)
    private static BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder()

    @Autowired
    private UserRepository repository;

    @PostConstruct
    void initUsers() {
        List<User> users = Stream.of(
                new User(100,"admin", passwordEncoder.encode("password"), "adrian.perkovic71@gmail.com", UserRole.ADMIN),
                new User(102, "agent_user", passwordEncoder.encode("kuracnabiciklu"), "", UserRole.AGENT),
        ).collect(Collectors.toList());
        repository.saveAll(users);


        LOG.info("Found users: " + repository.count() )
    }

    static void main(String[] args) {
        SpringApplication.run(ControllerApplication, args)
    }

}
