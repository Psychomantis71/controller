package eu.outerheaven.certmanager.controller

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan

@ConfigurationPropertiesScan
@SpringBootApplication
class ControllerApplication {

    static void main(String[] args) {
        SpringApplication.run(ControllerApplication, args)
    }

}
