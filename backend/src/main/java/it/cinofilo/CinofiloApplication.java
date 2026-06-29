package it.cinofilo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CinofiloApplication {

    public static void main(String[] args) {
        SpringApplication.run(CinofiloApplication.class, args);
    }
}
