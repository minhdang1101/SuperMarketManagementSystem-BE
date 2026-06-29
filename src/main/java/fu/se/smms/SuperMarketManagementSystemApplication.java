package fu.se.smms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SuperMarketManagementSystemApplication {
    private static final Logger log = LoggerFactory.getLogger(SuperMarketManagementSystemApplication.class);

    public static void main(String[] args) {
        log.info("Starting SuperMarketManagementSystem application...");
        SpringApplication.run(SuperMarketManagementSystemApplication.class, args);
        log.info("SuperMarketManagementSystem application started successfully");
    }
}
