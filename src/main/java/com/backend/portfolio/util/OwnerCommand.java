package com.backend.portfolio.util;

import com.backend.portfolio.config.OwnerProvisioningConfiguration;
import com.backend.portfolio.service.OwnerProvisioningService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.Console;
import java.util.Arrays;

public final class OwnerCommand {

    private OwnerCommand() {
    }

    public static boolean requested(String[] args) {
        return Arrays.stream(args).anyMatch(argument -> "--provision-owner".equals(argument)
                || "--reset-owner-password".equals(argument));
    }

    public static void run(String[] args) {
        if (args.length != 1 || !requested(args)) {
            throw new IllegalArgumentException("Use exactly one owner command; supply secrets through the console or environment");
        }
        Console console = System.console();
        String username = System.getenv("OWNER_USERNAME");
        if (username == null && console != null) {
            username = console.readLine("Owner username: ");
        }
        char[] password = readPassword(console);
        try {
            SpringApplication application = new SpringApplication(OwnerProvisioningConfiguration.class);
            application.setWebApplicationType(WebApplicationType.NONE);
            application.setAdditionalProfiles("owner-command");
            try (ConfigurableApplicationContext context = application.run()) {
                OwnerProvisioningService service = context.getBean(OwnerProvisioningService.class);
                if ("--provision-owner".equals(args[0])) {
                    service.provision(username, password);
                    System.out.println("Owner provisioned.");
                } else {
                    service.resetPassword(username, password);
                    System.out.println("Owner password reset; previous tokens are invalid.");
                }
            }
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static char[] readPassword(Console console) {
        if (console == null) {
            String password = System.getenv("OWNER_PASSWORD");
            if (password == null) {
                throw new IllegalArgumentException("A console or OWNER_PASSWORD environment value is required");
            }
            return password.toCharArray();
        }
        char[] password = console.readPassword("Owner password: ");
        char[] confirmation = null;
        boolean confirmed = false;
        try {
            confirmation = console.readPassword("Confirm password: ");
            if (password == null || confirmation == null || !Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("Passwords do not match");
            }
            confirmed = true;
            return password;
        } finally {
            if (!confirmed && password != null) {
                Arrays.fill(password, '\0');
            }
            if (confirmation != null) {
                Arrays.fill(confirmation, '\0');
            }
        }
    }
}
