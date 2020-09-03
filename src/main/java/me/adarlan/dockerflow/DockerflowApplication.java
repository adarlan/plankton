package me.adarlan.dockerflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DockerflowApplication {

	public static void main(String[] args) {
		Logger.init();
		Logger.debug(() -> " _____             _              __ _               ");
		Logger.debug(() -> "|  __ \\           | |            / _| |              ");
		Logger.debug(() -> "| |  | | ___   ___| | _____ _ __| |_| | _____      __");
		Logger.debug(() -> "| |  | |/ _ \\ / __| |/ / _ \\ '__|  _| |/ _ \\ \\ /\\ / /");
		Logger.debug(() -> "| |__| | (_) | (__|   <  __/ |  | | | | (_) \\ V  V / ");
		Logger.debug(() -> "|_____/ \\___/ \\___|_|\\_\\___|_|  |_| |_|\\___/ \\_/\\_/  ");
	   
		SpringApplication.run(DockerflowApplication.class, args);
	}
}
