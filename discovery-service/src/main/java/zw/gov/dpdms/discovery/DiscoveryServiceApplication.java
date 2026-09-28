package zw.gov.dpdms.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Service registry for the DPDMS microservices landscape.
 * Every hazard service, plus auth/report/alert/dashboard/gateway,
 * registers itself here so the gateway can discover them by name
 * instead of hardcoded host:port pairs.
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}
