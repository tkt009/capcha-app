package com.tk.spring.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CapchaAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(CapchaAppApplication.class, args);
		IO.println("Capcha App Started");
	}

}
