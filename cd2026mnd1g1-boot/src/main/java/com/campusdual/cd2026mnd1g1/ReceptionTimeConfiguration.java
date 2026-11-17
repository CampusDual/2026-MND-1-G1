package com.campusdual.cd2026mnd1g1;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Proporciona la hora oficial utilizada por las consultas de recepción.
 */
@Configuration
public class ReceptionTimeConfiguration {

    @Bean
    public Clock receptionClock() {
        return Clock.system(ZoneId.of("Europe/Madrid"));
    }
}
