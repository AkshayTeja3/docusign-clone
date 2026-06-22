package com.docusign.docusign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync; // 🎯 Import this!

@SpringBootApplication
@EnableAsync // 🔥 This activates the background thread pool for your @Async listeners!
public class DocusignApplication {
	public static void main(String[] args) {
		SpringApplication.run(DocusignApplication.class, args);
	}
}