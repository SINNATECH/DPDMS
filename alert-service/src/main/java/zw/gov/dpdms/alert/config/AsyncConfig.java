package zw.gov.dpdms.alert.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    /**
     * Dedicated thread pool for alert delivery, so a slow SMTP server or
     * WhatsApp API call only ever ties up these threads and never the web
     * threads handling incoming requests.
     */
    @Bean(name = "alertExecutor")
    public ThreadPoolTaskExecutor alertExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("alert-dispatch-");
        return executor;
    }
}
