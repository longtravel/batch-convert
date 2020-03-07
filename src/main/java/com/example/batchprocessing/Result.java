package com.example.batchprocessing;

public class Result {

  private int result;
  private String note;

  public Result (int result){
    this.result = result;
    this.note = "";
  }

  public void setResult(int result) {
    this.result = result;
  }

  public int getResult() {
    return result;
  }
  public void setNote(String note) {
    this.note = note;
  }

  public String getNote() {
    return note;
  }

  @Override
  public String toString() {
    return "result: " + result;
  }

}
