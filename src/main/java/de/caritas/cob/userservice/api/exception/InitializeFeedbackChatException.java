package de.caritas.cob.userservice.api.exception;

import de.caritas.cob.userservice.api.container.CreateEnquiryExceptionInformation;

public class InitializeFeedbackChatException extends CreateEnquiryException {

  private static final long serialVersionUID = -9002763989727764277L;

  /**
   * Exception when the initialization of a feedback chat fails
   * 
   * @param message
   */
  public InitializeFeedbackChatException(String message,
      CreateEnquiryExceptionInformation exceptionInformation) {
    super(message, exceptionInformation);
  }
}