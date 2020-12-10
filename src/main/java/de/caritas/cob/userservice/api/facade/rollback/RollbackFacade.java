package de.caritas.cob.userservice.api.facade.rollback;

import static java.util.Objects.nonNull;

import de.caritas.cob.userservice.api.service.MonitoringService;
import de.caritas.cob.userservice.api.service.SessionService;
import de.caritas.cob.userservice.api.service.UserAgencyService;
import de.caritas.cob.userservice.api.service.UserService;
import de.caritas.cob.userservice.api.service.helper.KeycloakAdminClientHelper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/*
 * Facade for capsuling the steps to roll back an user account.
 */
@Service
@RequiredArgsConstructor
public class RollbackFacade {

  private final @NonNull KeycloakAdminClientHelper keycloakAdminClientHelper;
  private final @NonNull UserAgencyService userAgencyService;
  private final @NonNull SessionService sessionService;
  private final @NonNull UserService userService;
  private final @NonNull MonitoringService monitoringService;

  /**
   * Deletes the provided user in Keycloak, MariaDB and its related session or user-chat/agency
   * relations depending on the provided {@link RollbackUserAccountInformation}.
   *
   * @param rollbackUser {@link RollbackUserAccountInformation}
   */
  public void rollBackUserAccount(RollbackUserAccountInformation rollbackUser) {
    rollbackUserAgency(rollbackUser);
    rollbackSession(rollbackUser);
    rollbackKeycloakAndMariaDbAccount(rollbackUser);
  }

  private void rollbackSession(RollbackUserAccountInformation rollbackUser) {
    if (nonNull(rollbackUser.getSession())) {
      sessionService.deleteSession(rollbackUser.getSession());
      monitoringService.rollbackInitializeMonitoring(rollbackUser.getSession());
    }
  }

  private void rollbackUserAgency(RollbackUserAccountInformation rollbackUser) {
    if (nonNull(rollbackUser.getUserAgency())) {
      userAgencyService.deleteUserAgency(rollbackUser.getUserAgency());
    }
  }

  private void rollbackKeycloakAndMariaDbAccount(RollbackUserAccountInformation rollbackUser) {
    if (rollbackUser.isRollBackUserAccount()) {
      if (nonNull(rollbackUser.getUserId())) {
        keycloakAdminClientHelper.rollBackUser(rollbackUser.getUserId());
      }
      if (nonNull(rollbackUser.getUser())) {
        userService.deleteUser(rollbackUser.getUser());
      }
    }
  }
}