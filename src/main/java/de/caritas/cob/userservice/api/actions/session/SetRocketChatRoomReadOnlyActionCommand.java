package de.caritas.cob.userservice.api.actions.session;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import de.caritas.cob.userservice.api.actions.ActionCommand;
import de.caritas.cob.userservice.api.repository.session.Session;
import de.caritas.cob.userservice.api.service.LogService;
import de.caritas.cob.userservice.api.service.rocketchat.RocketChatService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Sets the rooms to read only in Rocket.Chat.
 */
@Component
@RequiredArgsConstructor
public class SetRocketChatRoomReadOnlyActionCommand implements ActionCommand<Session> {

  private final @NonNull RocketChatService rocketChatService;

  /**
   * Sets the Rocket.Chat rooms to read only.
   *
   * @param session the session with groups to deactivate in Rocket.Chat.
   */
  @Override
  public void execute(Session session) {
    if (nonNull(session)) {
      setRoomReadOnly(session.getGroupId());
      setRoomReadOnly(session.getFeedbackGroupId());
    }
  }

  private void setRoomReadOnly(String rcRoomId) {
    if (isNotBlank(rcRoomId)) {
      try {
        this.rocketChatService.setRoomReadOnly(rcRoomId);
      } catch (Exception e) {
        LogService.logRocketChatError(e.getMessage());
      }
    }
  }

}
