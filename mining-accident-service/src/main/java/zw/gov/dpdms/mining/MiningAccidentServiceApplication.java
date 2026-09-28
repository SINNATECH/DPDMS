package zw.gov.dpdms.mining;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MiningAccidentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiningAccidentServiceApplication.class, args);
    }
}
