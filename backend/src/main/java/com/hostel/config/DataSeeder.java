package com.hostel.config;

import com.hostel.service.DemoDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Entry point for demo-data import.
 *
 * <p>Behavior is controlled by properties:
 * <ul>
 *   <li>{@code app.data-seeder.enabled} (default {@code true}) - master switch.</li>
 *   <li>{@code app.demo-data.reset} (default {@code false}) - pass
 *       {@code --app.demo-data.reset=true} to wipe and re-import the demo dataset.</li>
 *   <li>{@code app.demo-data.seed-on-empty} (default {@code true}) - automatically
 *       import the demo dataset when the database is empty.</li>
 * </ul>
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final DemoDataService demoDataService;

    @Value("${app.data-seeder.enabled:true}")
    private boolean enabled;

    @Value("${app.demo-data.reset:false}")
    private boolean reset;

    @Value("${app.demo-data.seed-on-empty:true}")
    private boolean seedOnEmpty;

    public DataSeeder(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.info("Demo data import disabled (app.data-seeder.enabled=false). Skipping.");
            return;
        }
        if (reset) {
            log.info("app.demo-data.reset=true -> wiping existing data and importing fresh demo dataset...");
            demoDataService.resetAndSeed();
            return;
        }
        if (seedOnEmpty && demoDataService.isEmpty()) {
            log.info("Database is empty -> importing demo dataset...");
            demoDataService.seedIfEmpty();
            return;
        }
        log.info("Demo data import skipped (reset=false and database already contains data).");
    }
}
