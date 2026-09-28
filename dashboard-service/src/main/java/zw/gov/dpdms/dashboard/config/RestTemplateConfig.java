package zw.gov.dpdms.dashboard.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * @LoadBalanced lets us call http://flood-service/api/floods and have
     * Spring Cloud LoadBalancer resolve "flood-service" to a real
     * host:port via Eureka — no hardcoded ports here, unlike web-client's
     * gateway-facing RestTemplate.
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
