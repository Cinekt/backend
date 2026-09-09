package com.jacobo.cinekt.iam.application.internal.eventhandlers;

import java.sql.Timestamp;

import com.jacobo.cinekt.iam.domain.model.commands.SeedAdminCommand;
import com.jacobo.cinekt.iam.domain.services.UserCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.jacobo.cinekt.iam.domain.model.commands.SeedRolesCommand;
import com.jacobo.cinekt.iam.domain.services.RoleCommandService;

@Service
public class ApplicationReadyEventHandler {

    @Value("${authorization.admin.email}")
    private String adminEmail;

    @Value("${authorization.admin.password}")
    private String adminPassword;

    private final RoleCommandService roleCommandService;
    private final UserCommandService userCommandService;
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationReadyEventHandler.class);

    public ApplicationReadyEventHandler(RoleCommandService roleCommandService, UserCommandService userCommandService) {
        this.roleCommandService = roleCommandService;
        this.userCommandService = userCommandService;
    }

    @EventListener
    public void on(ApplicationReadyEvent event) {
        var applicationName = event.getApplicationContext().getId();
        LOGGER.info("Starting to verify if roles seeding is needed for {} at {}", applicationName, currentTimestamp());
        var seedRolesCommand = new SeedRolesCommand();
        roleCommandService.handle(seedRolesCommand);
        var seedAdminCommand = new SeedAdminCommand(
                adminEmail,
                adminPassword
        );
        userCommandService.handle(seedAdminCommand);
        LOGGER.info("Roles seeding verification finished for {} at {}", applicationName, currentTimestamp());
    }

    private Timestamp currentTimestamp() {
        return new Timestamp(System.currentTimeMillis());
    }
}
