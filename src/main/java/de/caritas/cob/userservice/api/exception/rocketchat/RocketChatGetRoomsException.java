package de.caritas.cob.userservice.api.exception.rocketchat;

public class RocketChatGetRoomsException extends Exception {

  private static final long serialVersionUID = -6467348860210122736L;

  /**
   * Exception, when a Rocket.Chat API call to get subscriptions fails
   * 
   * @param ex
   */
  public RocketChatGetRoomsException(Exception ex) {
    super(ex);
  }

  public RocketChatGetRoomsException(String message) {
    super(message);
  }

}