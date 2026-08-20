package com.aegisterra.platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupVersionLogger implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupVersionLogger.class);

    private final String version;
    private final String applicationName;

    public StartupVersionLogger(
        @Value("${aegisterra.version:1.0.0}") String version,
        @Value("${spring.application.name:aegisterra-platform}") String applicationName
    ) {
        this.version = version;
        this.applicationName = applicationName;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting {} version {}", applicationName, version);
    }
}
