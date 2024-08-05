package com.verve.assessment.exceptions;

public class PromotionProcessingException extends RuntimeException {
  public PromotionProcessingException(String message) {
    super(message);
  }

  public PromotionProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}