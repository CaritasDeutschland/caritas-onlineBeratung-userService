package de.caritas.cob.userservice.api.exception.httpresponses;

import de.caritas.cob.userservice.api.service.LogService;
import java.util.function.Consumer;

public class InternalServerErrorException extends CustomHttpStatusException {

  /**
   * InternalServerError exception.
   *
   * @param message an additional message
   * @param loggingMethod the method being used to log this exception
   */
  public InternalServerErrorException(String message, Consumer<Exception> loggingMethod) {
    super(message, loggingMethod);
  }

  /**
   * InternalServerError exception.
   *
   * @param message an additional message
   */
  public InternalServerErrorException(String message) {
    super(message, LogService::logInternalServerError);
  }

}