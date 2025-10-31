package it.gls.dipendenti;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class Application {

	public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"));
        SpringApplication.run(Application.class, args);
	}

}
