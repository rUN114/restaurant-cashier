package com.restaurant.cashiersystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@MapperScan("com.restaurant.cashiersystem.mapper")
@EnableMethodSecurity(prePostEnabled = false)
public class RestaurantCashierSystemApplication {
	public static void main(String[] args) {
		SpringApplication.run(RestaurantCashierSystemApplication.class, args);
	}
}