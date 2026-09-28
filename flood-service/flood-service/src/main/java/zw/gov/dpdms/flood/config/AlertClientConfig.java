package zw.gov.dpdms.flood.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableAsync
public class AlertClientConfig {

    /**
     * @LoadBalanced lets us call http://alert-service/... and have it resolved
     * through Eureka. Short timeouts keep a stuck alert-service from tying up
     * the background thread for long.
     */
    @Bean
    @LoadBalanced
    public RestTemplate alertRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }
}
