package zw.gov.dpdms.fire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class FireServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FireServiceApplication.class, args);
    }
}
