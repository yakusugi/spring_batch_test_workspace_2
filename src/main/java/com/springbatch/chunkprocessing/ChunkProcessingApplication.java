package com.springbatch.chunkprocessing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = { "com.springbatch.controller", "com.springbatch.config" })
public class ChunkProcessingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChunkProcessingApplication.class, args);
	}

}
