package org.example.workload_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "spring.activemq")
@Component
@Data
public class ActiveMQProperties {
    private String brokerUrl;
    private String user;
    private String password;
}