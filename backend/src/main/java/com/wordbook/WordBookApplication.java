package com.wordbook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for the WordBook web application (single local {@code java -jar} artifact). */
@SpringBootApplication
public class WordBookApplication {

  public static void main(String[] args) {
    SpringApplication.run(WordBookApplication.class, args);
  }
}
