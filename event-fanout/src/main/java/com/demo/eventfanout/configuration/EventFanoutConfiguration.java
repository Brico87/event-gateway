package com.demo.eventfanout.configuration;

import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;
import java.util.function.Function;

@Configuration
public class EventFanoutConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventFanoutConfiguration.class);
    private static final String EVENT_1 = "event1";
    private static final String EVENT_2 = "event2";

    // TODO: stream.branch is deprecated => https://www.baeldung.com/kafka-splitting-streams
    @Bean("stream-processor")
    public Function<KStream<String, String>, KStream<String, String>[]> routingProcessor() {

        Predicate<String, String> isEvent1 = (k, v) -> k.startsWith(EVENT_1);
        Predicate<String, String> isEvent2 = (k, v) -> k.startsWith(EVENT_2);
        Predicate<String, String> isEventUnknown = (k, v) -> !(k.startsWith(EVENT_1) || k.startsWith(EVENT_2));

        return input -> input.branch(isEvent1,  isEvent2, isEventUnknown);
    }

    @Bean
    public Consumer<String> event1() {
        return data -> LOGGER.info("Data received from event-1: '{}'", data);
    }

    @Bean
    public Consumer<String> event2() {
        return data -> LOGGER.info("Data received from event-2: '{}'", data);
    }
}
