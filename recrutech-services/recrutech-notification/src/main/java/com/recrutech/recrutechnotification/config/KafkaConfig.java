package com.recrutech.recrutechnotification.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for the notification service.
 * Sets up consumer factory and listener container factory with proper settings.
 */
@EnableKafka
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:recrutech-notification}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    /**
     * Creates Kafka consumer factory with proper configuration.
     * @return Configured consumer factory
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // Basic configuration
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        
        // Deserializers
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        // Manual acknowledgment configuration
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        // Performance and reliability settings
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10); // Process fewer records at once for email sending
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 minutes - email sending can take time
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000); // 30 seconds
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000); // 10 seconds
        
        // Retry and error handling
        configProps.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, 1000); // 1 second
        configProps.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, 5000); // 5 seconds
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Creates Kafka listener container factory for @KafkaListener annotations.
     * @return Configured listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory());
        
        // Manual acknowledgment mode
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // Concurrency settings - limited for email sending
        factory.setConcurrency(2); // Only 2 concurrent consumers to avoid overwhelming SMTP
        
        // Error handling - don't stop container on errors
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler(
            (consumerRecord, exception) -> {
                // Log the error but don't stop processing
                System.err.println("[DEBUG_LOG] Error processing Kafka message: " + exception.getMessage());
                System.err.println("[DEBUG_LOG] Failed record: " + consumerRecord);
            },
            new org.springframework.util.backoff.FixedBackOff(5000L, 3L) // 5 seconds delay, 3 retries
        ));
        
        return factory;
    }
}