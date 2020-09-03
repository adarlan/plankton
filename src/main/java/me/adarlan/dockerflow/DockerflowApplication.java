package me.adarlan.dockerflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DockerflowApplication {

	public static void main(String[] args) {
		Logger.init();
		System.out.println(" _____             _              __ _               ");
		System.out.println("|  __ \\           | |            / _| |              ");
		System.out.println("| |  | | ___   ___| | _____ _ __| |_| | _____      __");
		System.out.println("| |  | |/ _ \\ / __| |/ / _ \\ '__|  _| |/ _ \\ \\ /\\ / /");
		System.out.println("| |__| | (_) | (__|   <  __/ |  | | | | (_) \\ V  V / ");
		System.out.println("|_____/ \\___/ \\___|_|\\_\\___|_|  |_| |_|\\___/ \\_/\\_/  ");

		SpringApplication.run(DockerflowApplication.class, args);
	}
}
