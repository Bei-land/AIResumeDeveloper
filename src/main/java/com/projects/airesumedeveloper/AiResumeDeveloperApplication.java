package com.projects.airesumedeveloper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class AiResumeDeveloperApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiResumeDeveloperApplication.class, args);
        log.info("server started");
    }
}
