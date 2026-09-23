package com.enjoyiot.eiot.message.kafka;


import com.enjoyiot.eiot.message.core.MqProducer;
import com.enjoyiot.eiot.message.kafka.partitioner.ProductKeyPartitioner;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

import static com.enjoyiot.eiot.message.core.ErrorCodeConstants.INIT_PRODUCER_ERROR;
import static com.enjoyiot.eiot.message.core.ErrorCodeConstants.SEND_MSG_ERROR;
import static com.enjoyiot.framework.common.exception.util.ServiceExceptionUtil.exception;

public class KafkaProducer<T> implements MqProducer<T> {

    private KafkaTemplate kafkaTemplate;

    public KafkaProducer(String bootstrapServers) {

        try {
            try {
                kafkaTemplate = createProducer(bootstrapServers, "iot-producer-" + System.currentTimeMillis());
            } catch (Throwable e) {
                throw exception(INIT_PRODUCER_ERROR);
            }
        } catch (Throwable e) {
            throw exception(INIT_PRODUCER_ERROR);
        }
    }

    @Override
    public void publish(String topic, T msg) {
        try {
            kafkaTemplate.send(new ProducerRecord<String, T>(topic, msg));
        } catch (Throwable e) {
            throw exception(SEND_MSG_ERROR);
        }
    }

    public static KafkaTemplate createProducer(String bootstrapServers, String clientId) {

        Map<String, Object> producerProps = new HashMap<>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        producerProps.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, ProductKeyPartitioner.class);

        DefaultKafkaProducerFactory<?, ?> kafkaProducerFactory = new DefaultKafkaProducerFactory<>(producerProps);
        KafkaTemplate<?, ?> kafkaTemplate = new KafkaTemplate<>(kafkaProducerFactory);

//        kafkaTemplate.setProducerListener(kafkaProducerListener);
        return kafkaTemplate;
    }

}
