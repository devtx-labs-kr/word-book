package com.wordbook.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/** Request to create a deck. {@code name} is required (VR-1). */
public class CreateDeckRequest {

  @NotBlank(message = "name must not be blank")
  private String name;

  @JsonProperty("descriptionText")
  private String description;

  private String color;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }
}
