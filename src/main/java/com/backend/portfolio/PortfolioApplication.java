package com.backend.portfolio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.backend.portfolio.util.OwnerCommand;

@SpringBootApplication
public class PortfolioApplication {

	public static void main(String[] args) {
		if (OwnerCommand.requested(args)) {
			OwnerCommand.run(args);
			return;
		}
		SpringApplication.run(PortfolioApplication.class, args);
	}

}
