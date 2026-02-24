package com.broadleafcommerce.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Search & Indexing microservice.
 * This standalone Spring Boot application wraps the search and indexing logic
 * previously embedded in the BroadleafCommerce monolith and exposes it as a REST API.
 */
@SpringBootApplication
public class SearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }
}
