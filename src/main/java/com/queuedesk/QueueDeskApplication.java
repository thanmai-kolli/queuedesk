package com.queuedesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts QueueDesk: the embedded web server, the database connection and every Spring bean.
 *
 * @SpringBooApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan.
 * Component scanning finds every @RestController, @Service, @Repository and @Component
 * in this package and the packages below it.
 */
@SpringBootApplication
public class QueueDeskApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueueDeskApplication.class, args);
    }
}
