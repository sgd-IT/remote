package com.enjoyiot.eiot.message.kafka.config;


import com.enjoyiot.eiot.common.thing.ThingModelMessage;
import com.enjoyiot.eiot.message.core.MqConsumer;
import com.enjoyiot.eiot.message.core.MqProducer;
import com.enjoyiot.eiot.message.kafka.KafkaProducer;
import com.enjoyiot.eiot.message.kafka.KafkaConsumer;
import com.enjoyiot.framework.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
@Slf4j
// 暂时还没测试,所以未开启
//@ConditionalOnProperty(name = "eiot.message.producer-type", havingValue = "kafka-event")
public class KafkaAutoConfiguration {

    private final KafkaProperties properties;

    public KafkaAutoConfiguration(KafkaProperties properties) {
        this.properties = properties;
    }

    @ConditionalOnMissingBean
    @Bean
    public MqProducer<ThingModelMessage> getThingModelMessageProducer() {
        log.info("初始化 kafka 生产者: {}", JsonUtils.toJsonString(properties));
        return new KafkaProducer<>(properties.getBoostrapServers());
    }

    @ConditionalOnMissingBean
    @Bean
    public MqConsumer<ThingModelMessage> getThingModelMessageConsumer() {
        log.info("初始化 kafka 消费者: {}", JsonUtils.toJsonString(properties));
        return new KafkaConsumer<>(properties.getBoostrapServers(), ThingModelMessage.class);
    }

}
