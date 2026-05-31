package bf.backend.housing_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class HousingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HousingServiceApplication.class, args);
    }

}
