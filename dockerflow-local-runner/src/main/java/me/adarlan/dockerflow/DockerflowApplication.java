package me.adarlan.dockerflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DockerflowApplication {

	public static void main(String[] args) {
		Logger.setLevel(Logger.Level.FOLLOW);
		SpringApplication.run(DockerflowApplication.class, args);
	}
}
