package com.meetmind.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MeetMindBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeetMindBackendApplication.class, args);
	}

}
