package com.wordbook.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to update a card's display fields. {@code front}/{@code back} are required (VR-2). SRS
 * fields are never touched on this path (DR-5).
 */
public class UpdateCardRequest {

  @NotBlank(message = "front must not be blank")
  private String front;

  @NotBlank(message = "back must not be blank")
  private String back;

  private String example;
  private String pronunciation;
  private String notes;

  public String getFront() {
    return front;
  }

  public void setFront(String front) {
    this.front = front;
  }

  public String getBack() {
    return back;
  }

  public void setBack(String back) {
    this.back = back;
  }

  public String getExample() {
    return example;
  }

  public void setExample(String example) {
    this.example = example;
  }

  public String getPronunciation() {
    return pronunciation;
  }

  public void setPronunciation(String pronunciation) {
    this.pronunciation = pronunciation;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
