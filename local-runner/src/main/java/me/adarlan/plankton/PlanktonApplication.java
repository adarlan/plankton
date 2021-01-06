package me.adarlan.plankton;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import me.adarlan.plankton.Logger;

@SpringBootApplication
public class PlanktonApplication {

	public static void main(String[] args) {
		Logger.setLevel(Logger.Level.DEBUG);
		SpringApplication.run(PlanktonApplication.class, args);
	}
}
