package de.caritas.cob.userservice.api.deleteworkflow.action.asker;

import static de.caritas.cob.userservice.api.deleteworkflow.model.DeletionSourceType.ASKER;
import static de.caritas.cob.userservice.api.deleteworkflow.model.DeletionTargetType.ANONYMOUS_REGISTRY_IDS;
import static de.caritas.cob.userservice.localdatetime.CustomLocalDateTime.nowInUtc;

import de.caritas.cob.userservice.api.actions.ActionCommand;
import de.caritas.cob.userservice.api.conversation.service.user.anonymous.AnonymousUsernameRegistry;
import de.caritas.cob.userservice.api.deleteworkflow.model.AskerDeletionWorkflowDTO;
import de.caritas.cob.userservice.api.deleteworkflow.model.DeletionWorkflowError;
import de.caritas.cob.userservice.api.service.LogService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Deletes a registry id by username.
 */
@Component
@RequiredArgsConstructor
public class DeleteAnonymousRegistryIdAction implements ActionCommand<AskerDeletionWorkflowDTO> {

  private final @NonNull AnonymousUsernameRegistry anonymousUsernameRegistry;

  /**
   * Deletes a registry id by username.
   *
   * @param actionTarget the {@link AskerDeletionWorkflowDTO}
   */
  @Override
  public void execute(AskerDeletionWorkflowDTO actionTarget) {
    try {
      anonymousUsernameRegistry.removeRegistryIdByUsername(actionTarget.getUser().getUsername());
    } catch (Exception e) {
      LogService.logDeleteWorkflowError(e);
      actionTarget.getDeletionWorkflowErrors().add(
          DeletionWorkflowError.builder()
              .deletionSourceType(ASKER)
              .deletionTargetType(ANONYMOUS_REGISTRY_IDS)
              .identifier(actionTarget.getUser().getUserId())
              .reason("Could not delete registry id for anonymous users by username")
              .timestamp(nowInUtc())
              .build()
      );
    }
  }
}
