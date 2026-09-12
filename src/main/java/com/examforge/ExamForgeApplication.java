package com.examforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ExamForge Backend - Entry point.
 *
 * Clean Architecture / Modular Monolith backend for an online
 * competitive-exam preparation platform.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableScheduling
public class ExamForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamForgeApplication.class, args);
    }
}
