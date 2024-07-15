package com.demo.eventfanout.configuration;

import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@Configuration
public class EventFanoutConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventFanoutConfiguration.class);
    private static final String EVENT_1 = "event1";
    private static final String EVENT_2 = "event2";

    @Bean("stream-processor")
    public Function<KStream<String, String>, KStream<String, String>[]> routingProcessor() {

        Predicate<String, String> isEvent1 = (k, v) -> k.startsWith(EVENT_1);
        Predicate<String, String> isEvent2 = (k, v) -> k.startsWith(EVENT_2);
        Predicate<String, String> isEventUnknown = (k, v) -> !(k.startsWith(EVENT_1) || k.startsWith(EVENT_2));

        return input -> {
            final Map<String, KStream<String, String>> stringKStreamMap = input
                    .split()
                    .branch(isEvent1, Branched.as(EVENT_1 + "_topic"))
                    .branch(isEvent2, Branched.as(EVENT_2 + "_topic"))
                    .branch(isEventUnknown)
                    .noDefaultBranch();

            return stringKStreamMap.values().toArray(new KStream[0]);
        };
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
