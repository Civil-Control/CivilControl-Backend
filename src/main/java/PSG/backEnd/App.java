package PSG.backEnd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class
 * EnableAsync annotation enables asynchronous processing for methods annotated with Async
 * EnableScheduling enables @Scheduled tasks (e.g. JWT blacklist purge)
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class App {

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

}
