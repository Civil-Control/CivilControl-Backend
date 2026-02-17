package PSG.backEnd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class
 * EnableAsync annotation enables asynchronous processing for methods annotated with Async
 */
@SpringBootApplication
@EnableAsync
public class App {

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

}
