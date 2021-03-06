package de.caritas.cob.userservice.testConfig;

import de.caritas.cob.userservice.api.admin.service.consultant.validation.UserAccountInputValidator;
import de.caritas.cob.userservice.api.authorization.UserRole;
import de.caritas.cob.userservice.api.helper.AuthenticatedUser;
import de.caritas.cob.userservice.api.helper.UserHelper;
import de.caritas.cob.userservice.api.model.keycloak.KeycloakCreateUserResponseDTO;
import de.caritas.cob.userservice.api.model.keycloak.login.KeycloakLoginResponseDTO;
import de.caritas.cob.userservice.api.model.registration.UserDTO;
import de.caritas.cob.userservice.api.service.KeycloakService;
import de.caritas.cob.userservice.api.service.helper.KeycloakAdminClientAccessor;
import de.caritas.cob.userservice.api.service.helper.KeycloakAdminClientService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

@TestConfiguration
public class KeycloakTestConfig {

  @Bean
  public KeycloakAdminClientService keycloakAdminClientService(UserHelper userHelper,
      KeycloakAdminClientAccessor keycloakAdminClientAccessor) {
    return new KeycloakAdminClientService(userHelper, keycloakAdminClientAccessor) {
      @Override
      public KeycloakCreateUserResponseDTO createKeycloakUser(UserDTO user) {
        KeycloakCreateUserResponseDTO keycloakUserDTO = new KeycloakCreateUserResponseDTO();
        keycloakUserDTO.setUserId("keycloak-user-id");
        keycloakUserDTO.setStatus(HttpStatus.OK);

        return keycloakUserDTO;
      }

      @Override
      public String updateDummyEmail(String userId, UserDTO user) {
        var dummyMail = userId + "@dummy.du";
        user.setEmail(dummyMail);
        return dummyMail;
      }

      @Override
      public void updateUserRole(String userId) {
      }

      @Override
      public void updateRole(String userId, UserRole role) {
      }

      @Override
      public void updateRole(String userId, String roleName) {
      }

      @Override
      public void updatePassword(String userId, String password) {
      }

      @Override
      public void updateUserData(String userId, UserDTO userDTO, String firstName,
          String lastName) {
      }

      @Override
      public void updateEmail(String userId, String emailAddress) {
      }

      @Override
      public void rollBackUser(String userId) {
      }

      @Override
      public void deleteUser(String userId) {
      }

      @Override
      public void deactivateUser(String userId) {
      }
    };
  }

  @Bean
  public KeycloakService keycloakService(RestTemplate restTemplate,
      AuthenticatedUser authenticatedUser,
      KeycloakAdminClientService keycloakAdminClientService,
      UserAccountInputValidator userAccountInputValidator) {
    return new KeycloakService(restTemplate, authenticatedUser, keycloakAdminClientService,
        userAccountInputValidator) {
      @Override
      public boolean changePassword(String userId, String password) {
        return super.changePassword(userId, password);
      }

      @Override
      public KeycloakLoginResponseDTO loginUser(String userName, String password) {
        return new KeycloakLoginResponseDTO("", 0, 0, "", "", "", "");
      }

      @Override
      public boolean logoutUser(String refreshToken) {
        return true;
      }

      @Override
      public void changeEmailAddress(String emailAddress) {
      }
    };
  }
}
