package de.caritas.cob.userservice.api.exception.rocketchat;

import de.caritas.cob.userservice.api.container.CreateEnquiryExceptionInformation;
import de.caritas.cob.userservice.api.exception.CreateEnquiryException;

public class RocketChatAddConsultantsAndTechUserException extends CreateEnquiryException {

  private static final long serialVersionUID = -3027804676762081926L;

  /**
   * Exception when adding consultants and the technical user to a Rocket.Chat group fails.
   * 
   * @param message Error Message
   * @param exception Exception
   * @param exceptionInformation {@link CreateEnquiryExceptionInformation}
   */
  public RocketChatAddConsultantsAndTechUserException(String message, Exception exception,
      CreateEnquiryExceptionInformation exceptionInformation) {
    super(message, exception, exceptionInformation);
  }
}