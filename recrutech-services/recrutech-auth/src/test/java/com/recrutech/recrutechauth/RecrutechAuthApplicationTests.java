package com.recrutech.recrutechauth;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {
    RedisAutoConfiguration.class, 
    RedisRepositoriesAutoConfiguration.class,
    LiquibaseAutoConfiguration.class
})
class RecrutechAuthApplicationTests {

    @TestConfiguration
    static class TestConfig {
        
        @Bean
        @Primary
        @SuppressWarnings("unchecked")
        public RedisTemplate<String, String> redisTemplate() {
            RedisTemplate<String, String> mockRedisTemplate = Mockito.mock(RedisTemplate.class);
            org.springframework.data.redis.core.ValueOperations<String, String> mockValueOps = Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
            Mockito.when(mockRedisTemplate.opsForValue()).thenReturn(mockValueOps);
            return mockRedisTemplate;
        }
    }

    @Test
    void contextLoads() {
    }

}
