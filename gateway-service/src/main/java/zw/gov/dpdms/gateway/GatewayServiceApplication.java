package zw.gov.dpdms.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Single entry point for the front end and external clients.
 * Routing rules live in application.yml — this class stays empty
 * on purpose. Authorization is NOT done here; each downstream
 * service enforces its own hazard/role scoping.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}
