package zw.gov.dpdms.zoonotic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ZoonoticDiseaseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZoonoticDiseaseServiceApplication.class, args);
    }
}
