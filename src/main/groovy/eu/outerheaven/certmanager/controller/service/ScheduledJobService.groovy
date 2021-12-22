package eu.outerheaven.certmanager.controller.service

import eu.outerheaven.certmanager.controller.entity.Keystore
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import java.time.LocalDateTime


@Component
class ScheduledJobService {

    @Autowired
    private Environment environment

    @Autowired
    KeystoreService keystoreService

    @Autowired
    CaVaultService caVaultService
    private static final Logger LOG = LoggerFactory.getLogger(ScheduledJobService.class)

    @Scheduled(cron = "\${controller.expiration.check.period}")
    public void run() {
        caVaultService.scheduledCheck()
        keystoreService.scheduledCheck()
    }

}
