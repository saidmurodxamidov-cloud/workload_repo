package org.example.workload_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableJms
public class ActiveMQConfig {

    @Bean
    public ActiveMQConnectionFactory connectionFactory(
            @Value("${spring.activemq.broker-url}") String brokerUrl,
            @Value("${spring.activemq.user}") String user,
            @Value("${spring.activemq.password}") String password) {

        ActiveMQConnectionFactory factory =
                new ActiveMQConnectionFactory(user, password, brokerUrl);
        factory.setTrustedPackages(List.of("org.example.dto", "org.example.client",
                "org.example.workload_service.dto"));
        return factory;
    }

    @Bean
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");

        Map<String, Class<?>> typeIdMappings = new HashMap<>();
        typeIdMappings.put("TrainerWorkloadRequest", org.example.workload_service.dto.TrainerWorkloadRequest.class);
        converter.setTypeIdMappings(typeIdMappings);

        // 2. Configure Jackson to handle LocalDate
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        converter.setObjectMapper(mapper);

        return converter;
    }

    @Bean
    public JmsTemplate jmsTemplate(ActiveMQConnectionFactory connectionFactory,
                                   MessageConverter messageConverter) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setDeliveryPersistent(true);
        template.setSessionTransacted(true);
        return template;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ActiveMQConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        DefaultJmsListenerContainerFactory factory =
                new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setSessionTransacted(true);
        factory.setConcurrency("1-5");
        return factory;
    }
}