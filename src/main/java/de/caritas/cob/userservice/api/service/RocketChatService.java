package de.caritas.cob.userservice.api.service;

import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;

import de.caritas.cob.userservice.api.container.RocketChatCredentials;
import de.caritas.cob.userservice.api.exception.httpresponses.InternalServerErrorException;
import de.caritas.cob.userservice.api.exception.httpresponses.UnauthorizedException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatAddUserToGroupException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatCreateGroupException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatGetGroupMembersException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatGetRoomsException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatGetSubscriptionsException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatGetUserInfoException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatLoginException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatRemoveSystemMessagesException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatRemoveUserFromGroupException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatUserNotInitializedException;
import de.caritas.cob.userservice.api.model.rocketChat.StandardResponseDTO;
import de.caritas.cob.userservice.api.model.rocketChat.group.GroupAddUserBodyDTO;
import de.caritas.cob.userservice.api.model.rocketChat.group.GroupCleanHistoryDTO;
import de.caritas.cob.userservice.api.model.rocketChat.group.GroupCreateBodyDTO;
import de.caritas.cob.userservice.api.model.rocketChat.group.GroupDeleteBodyDTO;
import de.caritas.cob.userservice.api.model.rocketChat.group.GroupDeleteResponseDTO;
import de.caritas.cob.userservice.api.model.rocketChat.group.GroupMemberDTO;
import de.caritas.cob.userservice.api.model.rocketChat.group.GroupMemberResponseDTO;
import de.caritas.cob.userservice.api.model.rocketChat.group.GroupRemoveUserBodyDTO;
import de.caritas.cob.userservice.api.model.rocketChat.group.GroupResponseDTO;
import de.caritas.cob.userservice.api.model.rocketChat.login.LdapLoginDTO;
import de.caritas.cob.userservice.api.model.rocketChat.login.LoginResponseDTO;
import de.caritas.cob.userservice.api.model.rocketChat.logout.LogoutResponseDTO;
import de.caritas.cob.userservice.api.model.rocketChat.room.RoomsGetDTO;
import de.caritas.cob.userservice.api.model.rocketChat.room.RoomsUpdateDTO;
import de.caritas.cob.userservice.api.model.rocketChat.subscriptions.SubscriptionsGetDTO;
import de.caritas.cob.userservice.api.model.rocketChat.subscriptions.SubscriptionsUpdateDTO;
import de.caritas.cob.userservice.api.model.rocketChat.user.UserInfoResponseDTO;
import de.caritas.cob.userservice.api.service.helper.RocketChatCredentialsHelper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Service for Rocket.Chat functionalities
 */
@Getter
@Service
public class RocketChatService {

  private static final String ERROR_MESSAGE = "Error during rollback: Rocket.Chat group with id "
      + "%s could not be deleted";

  @Value("${rocket.chat.header.auth.token}")
  private String rocketChatHeaderAuthToken;

  @Value("${rocket.chat.header.user.id}")
  private String rocketChatHeaderUserId;

  @Value("${rocket.chat.api.group.create.url}")
  private String rocketChatApiGroupCreateUrl;

  @Value("${rocket.chat.api.group.delete.url}")
  private String rocketChatApiGroupDeleteUrl;

  @Value("${rocket.chat.api.group.add.user}")
  private String rocketChatApiGroupAddUserUrl;

  @Value("${rocket.chat.api.group.remove.user}")
  private String rocketChatApiGroupRemoveUserUrl;

  @Value("${rocket.chat.api.group.get.member}")
  private String rocketChatApiGetGroupMembersUrl;

  @Value("${rocket.chat.api.subscriptions.get}")
  private String rocketChatApiSubscriptionsGet;

  @Value("${rocket.chat.api.rooms.get}")
  private String rocketChatApiRoomsGet;

  @Value("${rocket.chat.api.user.login}")
  private String rocketChatApiUserLogin;

  @Value("${rocket.chat.api.user.logout}")
  private String rocketChatApiUserLogout;

  @Value("${rocket.chat.api.user.info}")
  private String rocketChatApiUserInfo;

  @Value("${rocket.chat.api.rooms.clean.history}")
  private String rocketChatApiCleanRoomHistory;

  private String rcDateTimePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
  private final LocalDateTime localDateTime1900 = LocalDateTime.of(1900, 01, 01, 00, 00);
  private final LocalDateTime localDateTimeFuture = LocalDateTime.now().plusYears(1L);

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private RocketChatCredentialsHelper rcCredentialHelper;

  /**
   * Creation of a private Rocket.Chat group.
   *
   * @param rocketChatCredentials {@link RocketChatCredentials}
   * @return the group id
   */
  public Optional<GroupResponseDTO> createPrivateGroup(String name,
      RocketChatCredentials rocketChatCredentials) throws RocketChatCreateGroupException {

    GroupResponseDTO response;

    try {

      HttpHeaders headers = getStandardHttpHeaders(rocketChatCredentials);
      GroupCreateBodyDTO groupCreateBodyDto = new GroupCreateBodyDTO(name, false);
      HttpEntity<GroupCreateBodyDTO> request =
          new HttpEntity<>(groupCreateBodyDto, headers);
      response =
          restTemplate.postForObject(rocketChatApiGroupCreateUrl, request, GroupResponseDTO.class);

    } catch (Exception ex) {
      LogService.logRocketChatError(
          String.format("Rocket.Chat group with name %s could not be created", name), ex);
      throw new RocketChatCreateGroupException(ex);
    }

    if (response != null && response.isSuccess() && isGroupIdAvailable(response)) {
      return Optional.of(response);
    } else {
      LogService.logRocketChatError(
          String.format("Rocket.Chat group with name %s could not be created", name),
          response.getError(), response.getErrorType());
      return Optional.empty();
    }

  }

  /**
   * Creates a private Rocket.Chat group with the system user (credentials)
   */
  public Optional<GroupResponseDTO> createPrivateGroupWithSystemUser(String groupName)
      throws RocketChatCreateGroupException {

    try {
      RocketChatCredentials systemUserCredentials = rcCredentialHelper.getSystemUser();
      return this.createPrivateGroup(groupName, systemUserCredentials);
    } catch (RocketChatUserNotInitializedException e) {
      throw new RocketChatCreateGroupException(e);
    }
  }

  /**
   * Deletion of a Rocket.Chat group as system user.
   *
   * @return true, if successfully
   */
  public boolean deleteGroupAsSystemUser(String groupId) {
    try {
      RocketChatCredentials systemUser = rcCredentialHelper.getSystemUser();
      return rollbackGroup(groupId, systemUser);
    } catch (RocketChatUserNotInitializedException e) {
      throw new InternalServerErrorException(e.getMessage(), LogService::logRocketChatError);
    }
  }

  /**
   * Deletion of a Rocket.Chat group.
   *
   * @param groupId the group id
   * @param rocketChatCredentials {@link RocketChatCredentials}
   * @return true, if successfully
   */
  public boolean rollbackGroup(String groupId, RocketChatCredentials rocketChatCredentials) {

    GroupDeleteResponseDTO response = null;

    try {

      HttpHeaders headers = getStandardHttpHeaders(rocketChatCredentials);
      GroupDeleteBodyDTO groupDeleteBodyDto = new GroupDeleteBodyDTO(groupId);
      HttpEntity<GroupDeleteBodyDTO> request =
          new HttpEntity<>(groupDeleteBodyDto, headers);
      response = restTemplate.postForObject(rocketChatApiGroupDeleteUrl, request,
          GroupDeleteResponseDTO.class);

    } catch (Exception ex) {
      LogService.logRocketChatError(String.format(ERROR_MESSAGE, groupId), ex);
    }

    if (response != null && response.isSuccess()) {
      return true;
    } else {
      LogService.logRocketChatError(String.format(ERROR_MESSAGE, groupId), "unknown", "unknown");
      return false;
    }

  }

  /**
   * Returns true if the group id is available in the {@link GroupResponseDTO}
   *
   * @return true, if group id is available
   */
  private boolean isGroupIdAvailable(GroupResponseDTO response) {
    return response.getGroup() != null && response.getGroup().getId() != null;
  }


  /**
   * Returns a HttpHeaders instance with standard settings (Rocket.Chat-Token, Rocket.Chat-User-ID,
   * MediaType)
   *
   * @param rocketChatCredentials {@link RocketChatCredentials}
   * @return a HttpHeaders instance with the standard settings
   */
  private HttpHeaders getStandardHttpHeaders(RocketChatCredentials rocketChatCredentials) {

    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
    httpHeaders.add(rocketChatHeaderAuthToken, rocketChatCredentials.getRocketChatToken());
    httpHeaders.add(rocketChatHeaderUserId, rocketChatCredentials.getRocketChatUserId());
    return httpHeaders;
  }

  /**
   * Retrieves the userId for the given credentials.
   *
   * @param username the username
   * @param password the password
   * @param firstLogin true, if first login in Rocket.Chat. This requires a special API call.
   * @return the userid
   * @throws {@link RocketChatLoginException}
   */
  public String getUserID(String username, String password, boolean firstLogin)
      throws RocketChatLoginException {

    ResponseEntity<LoginResponseDTO> response;

    if (firstLogin) {
      response = loginUserFirstTime(username, password);
    } else {
      response = loginUser(username, password);
    }

    RocketChatCredentials rocketChatCredentials =
        RocketChatCredentials.builder().RocketChatUserId(response.getBody().getData().getUserId())
            .RocketChatToken(response.getBody().getData().getAuthToken()).build();

    logoutUser(rocketChatCredentials);

    return rocketChatCredentials.getRocketChatUserId();
  }

  public ResponseEntity<LoginResponseDTO> loginUserFirstTime(String username, String password)
      throws RocketChatLoginException {

    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      LdapLoginDTO ldapLoginDTO = new LdapLoginDTO();
      ldapLoginDTO.setLdap(true);
      ldapLoginDTO.setUsername(username);
      ldapLoginDTO.setLdapPass(password);

      HttpEntity<LdapLoginDTO> request = new HttpEntity<>(ldapLoginDTO, headers);

      return restTemplate.postForEntity(rocketChatApiUserLogin, request, LoginResponseDTO.class);
    } catch (Exception ex) {
      throw new RocketChatLoginException(
          String.format("Could not login user (%s) in Rocket.Chat for the first time", username));
    }
  }

  /**
   * Performs a login with the given credentials and returns the Result.
   *
   * @param username the username
   * @param password the password
   * @return the response entity of the login dto
   * @throws {@link RocketChatLoginException}
   */
  public ResponseEntity<LoginResponseDTO> loginUser(String username, String password)
      throws RocketChatLoginException {

    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
      map.add("username", username);
      map.add("password", password);

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

      return restTemplate.postForEntity(rocketChatApiUserLogin, request, LoginResponseDTO.class);
    } catch (Exception ex) {
      throw new RocketChatLoginException(
          String.format("Could not login user (%s) in Rocket.Chat", username));
    }
  }

  /**
   * Performs a logout with the given credentials and returns true on success.
   *
   * @param rocketChatCredentials {@link RocketChatCredentials}
   * @return true if logout was successful
   */
  public boolean logoutUser(RocketChatCredentials rocketChatCredentials) {

    try {
      HttpHeaders headers = getStandardHttpHeaders(rocketChatCredentials);

      HttpEntity<Void> request = new HttpEntity<Void>(headers);

      ResponseEntity<LogoutResponseDTO> response =
          restTemplate.postForEntity(rocketChatApiUserLogout, request, LogoutResponseDTO.class);

      return response != null && response.getStatusCode() == HttpStatus.OK ? true : false;

    } catch (Exception ex) {
      LogService.logRocketChatError(String.format("Could not log out user id (%s) from Rocket.Chat",
          rocketChatCredentials.getRocketChatUserId()), ex);

      return false;
    }
  }

  /**
   * Adds the provided user to the Rocket.Chat group with given groupId
   *
   * @param rcUserId Rocket.Chat userId
   * @param rcGroupId Rocket.Chat roomId
   */
  public void addUserToGroup(String rcUserId, String rcGroupId)
      throws RocketChatAddUserToGroupException {

    GroupResponseDTO response;
    try {
      RocketChatCredentials technicalUser = rcCredentialHelper.getTechnicalUser();
      HttpHeaders header = getStandardHttpHeaders(technicalUser);
      GroupAddUserBodyDTO body = new GroupAddUserBodyDTO(rcUserId, rcGroupId);
      HttpEntity<GroupAddUserBodyDTO> request = new HttpEntity<>(body, header);

      response =
          restTemplate.postForObject(rocketChatApiGroupAddUserUrl, request, GroupResponseDTO.class);

    } catch (Exception ex) {
      throw new RocketChatAddUserToGroupException(String.format(
          "Could not add user %s to Rocket.Chat group with id %s", rcUserId, rcGroupId));
    }

    if (response != null && !response.isSuccess()) {
      String error = "Could not add user %s to Rocket.Chat group with id %s";
      throw new RocketChatAddUserToGroupException(String.format(error, rcUserId, rcGroupId));
    }
  }

  /**
   * Adds the technical user to the given Rocket.Chat group id.
   *
   * @param rcGroupId the rocket chat group id
   */
  public void addTechnicalUserToGroup(String rcGroupId)
      throws RocketChatAddUserToGroupException, RocketChatUserNotInitializedException {
    this.addUserToGroup(rcCredentialHelper.getTechnicalUser().getRocketChatUserId(), rcGroupId);
  }

  /**
   * Removes the provided user from the Rocket.Chat group with given groupId.
   *
   * @param rcUserId Rocket.Chat userId
   * @param rcGroupId Rocket.Chat roomId
   * @throws {@link RocketChatRemoveUserFromGroupException}
   */
  public void removeUserFromGroup(String rcUserId, String rcGroupId)
      throws RocketChatRemoveUserFromGroupException {

    GroupResponseDTO response;
    try {
      RocketChatCredentials technicalUser = rcCredentialHelper.getTechnicalUser();
      HttpHeaders header = getStandardHttpHeaders(technicalUser);
      GroupRemoveUserBodyDTO body = new GroupRemoveUserBodyDTO(rcUserId, rcGroupId);
      HttpEntity<GroupRemoveUserBodyDTO> request =
          new HttpEntity<>(body, header);

      response = restTemplate.postForObject(rocketChatApiGroupRemoveUserUrl, request,
          GroupResponseDTO.class);

    } catch (Exception ex) {
      throw new RocketChatRemoveUserFromGroupException(String.format(
          "Could not remove user %s from Rocket.Chat group with id %s", rcUserId, rcGroupId));
    }

    if (response != null && !response.isSuccess()) {
      String error = "Could not remove user %s from Rocket.Chat group with id %s";
      throw new RocketChatRemoveUserFromGroupException(String.format(error, rcUserId, rcGroupId));
    }
  }

  /**
   * Removes the technical user from the given Rocket.Chat group id.
   *
   * @param rcGroupId the rocket chat group id
   */
  public void removeTechnicalUserFromGroup(String rcGroupId)
      throws RocketChatRemoveUserFromGroupException, RocketChatUserNotInitializedException {
    this.removeUserFromGroup(rcCredentialHelper.getTechnicalUser().getRocketChatUserId(),
        rcGroupId);
  }

  /**
   * Get all standard members (all users except system user and technical user) of a rocket chat
   * group.
   *
   * @param rcGroupId the rocket chat group id
   * @return all standard members of that group
   */
  public List<GroupMemberDTO> getStandardMembersOfGroup(String rcGroupId)
      throws RocketChatGetGroupMembersException, RocketChatUserNotInitializedException {

    List<GroupMemberDTO> groupMemberList =
        new ArrayList<>(getMembersOfGroup(rcGroupId));

    if (groupMemberList.isEmpty()) {
      throw new RocketChatGetGroupMembersException(
          String.format("Group member list from group with id %s is empty", rcGroupId));
    }

    Iterator<GroupMemberDTO> groupMemberListIterator = groupMemberList.iterator();
    while (groupMemberListIterator.hasNext()) {
      GroupMemberDTO groupMemberDTO = groupMemberListIterator.next();
      if (groupMemberDTO.get_id()
          .equals(rcCredentialHelper.getTechnicalUser().getRocketChatUserId())
          || groupMemberDTO.get_id()
          .equals(rcCredentialHelper.getSystemUser().getRocketChatUserId())) {
        groupMemberListIterator.remove();
      }
    }

    return groupMemberList;

  }

  /**
   * Removes all users from the given group except system user and technical user.
   *
   * @param rcGroupId the rocket chat group id
   */
  public void removeAllStandardUsersFromGroup(String rcGroupId)
      throws RocketChatGetGroupMembersException, RocketChatRemoveUserFromGroupException, RocketChatUserNotInitializedException {
    List<GroupMemberDTO> groupMemberList = getMembersOfGroup(rcGroupId);

    if (groupMemberList.isEmpty()) {
      throw new RocketChatGetGroupMembersException(
          String.format("Group member list from group with id %s is empty", rcGroupId));
    }

    for (GroupMemberDTO member : groupMemberList) {
      if (!member.get_id().equals(rcCredentialHelper.getTechnicalUser().getRocketChatUserId())
          && !member.get_id().equals(rcCredentialHelper.getSystemUser().getRocketChatUserId())) {
        removeUserFromGroup(member.get_id(), rcGroupId);
      }
    }
  }

  /**
   * Returns the group/room members of the given Rocket.Chat group id.
   *
   * @param rcGroupId the rocket chat id
   * @return al members of the group
   */
  public List<GroupMemberDTO> getMembersOfGroup(String rcGroupId)
      throws RocketChatGetGroupMembersException {

    ResponseEntity<GroupMemberResponseDTO> response;
    try {
      RocketChatCredentials systemUser = rcCredentialHelper.getSystemUser();
      HttpHeaders header = getStandardHttpHeaders(systemUser);
      HttpEntity<GroupAddUserBodyDTO> request = new HttpEntity<GroupAddUserBodyDTO>(header);

      response = restTemplate.exchange(rocketChatApiGetGroupMembersUrl + "?roomId=" + rcGroupId,
          HttpMethod.GET, request, GroupMemberResponseDTO.class);

    } catch (Exception ex) {
      throw new RocketChatGetGroupMembersException(String.format("Could not get Rocket.Chat group"
          + " members for room id %s", rcGroupId), ex);
    }

    if (response != null && response.getStatusCode() == HttpStatus.OK) {
      return Arrays.asList(response.getBody().getMembers());
    } else {
      String error = "Could not get Rocket.Chat group members for room id %s";
      throw new RocketChatGetGroupMembersException(String.format(error, rcGroupId));
    }
  }

  /**
   * Removes all messages from the specified Rocket.Chat group written by the technical user from
   * the last 24 hours (avoiding time zone failures).
   *
   * @param rcGroupId the rocket chat group id
   * @param oldest the oldest message time
   * @param latest the latest message time
   */
  public void removeSystemMessages(String rcGroupId, LocalDateTime oldest, LocalDateTime latest)
      throws RocketChatRemoveSystemMessagesException, RocketChatUserNotInitializedException {
    RocketChatCredentials technicalUser = rcCredentialHelper.getTechnicalUser();
    this.removeMessages(rcGroupId, new String[]{technicalUser.getRocketChatUsername()}, oldest,
        latest);
  }

  /**
   * Removes all messages (from every user) from a Rocket.Chat group.
   *
   * @param rcGroupId the rocket chat group id
   */
  public void removeAllMessages(String rcGroupId)
      throws RocketChatRemoveSystemMessagesException {
    removeMessages(rcGroupId, null, localDateTime1900, localDateTimeFuture);
  }

  /**
   * Removes all messages from the specified Rocket.Chat group written by the given user name array
   * which have been written between oldest and latest ({@link LocalDateTime}.
   *
   * @param rcGroupId Rocket.Chat group id
   * @param users Array of usernames (String); null for all users
   * @param oldest {@link LocalDateTime}
   * @param latest {@link LocalDateTime}
   */
  private void removeMessages(String rcGroupId, String[] users, LocalDateTime oldest,
      LocalDateTime latest) throws RocketChatRemoveSystemMessagesException {

    StandardResponseDTO response;
    try {
      RocketChatCredentials technicalUser = rcCredentialHelper.getTechnicalUser();
      HttpHeaders header = getStandardHttpHeaders(technicalUser);
      GroupCleanHistoryDTO body = new GroupCleanHistoryDTO(rcGroupId,
          oldest.format(DateTimeFormatter.ofPattern(rcDateTimePattern)),
          latest.format(DateTimeFormatter.ofPattern(rcDateTimePattern)),
          (isNotEmpty(users)) ? users : new String[]{});
      HttpEntity<GroupCleanHistoryDTO> request = new HttpEntity<>(body, header);

      response = restTemplate.postForObject(rocketChatApiCleanRoomHistory, request,
          StandardResponseDTO.class);

    } catch (Exception ex) {
      throw new RocketChatRemoveSystemMessagesException(
          String.format("Could not clean history of Rocket.Chat group id %s", rcGroupId));
    }

    if (response != null && !response.isSuccess()) {
      throw new RocketChatRemoveSystemMessagesException(
          String.format("Could not clean history of Rocket.Chat group id %s", rcGroupId));
    }
  }

  /**
   * Returns the subscriptions for the given user id.
   *
   * @param rocketChatCredentials {@link RocketChatCredentials}
   * @return the subscriptions of the user
   */
  public List<SubscriptionsUpdateDTO> getSubscriptionsOfUser(
      RocketChatCredentials rocketChatCredentials) throws RocketChatGetSubscriptionsException {

    ResponseEntity<SubscriptionsGetDTO> response = null;

    try {
      HttpHeaders header = getStandardHttpHeaders(rocketChatCredentials);
      HttpEntity<Void> request = new HttpEntity<>(header);

      response = restTemplate.exchange(rocketChatApiSubscriptionsGet, HttpMethod.GET, request,
          SubscriptionsGetDTO.class);

    } catch (HttpStatusCodeException ex) {
      if (ex.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
        throw new UnauthorizedException(String.format(
            "Could not get Rocket.Chat subscriptions for user ID %s: Token is not active (401 Unauthorized)",
            rocketChatCredentials.getRocketChatUserId()));
      }
      throw new RocketChatGetSubscriptionsException(
          String.format("Could not get Rocket.Chat subscriptions for user id %s",
              rocketChatCredentials.getRocketChatUserId()));
    }

    if (response.getStatusCode() == HttpStatus.OK) {
      return Arrays.asList(response.getBody().getUpdate());
    } else {
      String error = "Could not get Rocket.Chat subscriptions for user id %s";
      throw new RocketChatGetSubscriptionsException(
          String.format(error, rocketChatCredentials.getRocketChatUserId()));
    }
  }

  /**
   * Returns the rooms for the given user id.
   *
   * @param rocketChatCredentials {@link RocketChatCredentials}
   * @return the rooms for the user
   */
  public List<RoomsUpdateDTO> getRoomsOfUser(RocketChatCredentials rocketChatCredentials)
      throws RocketChatGetRoomsException {

    ResponseEntity<RoomsGetDTO> response;

    try {
      HttpHeaders header = getStandardHttpHeaders(rocketChatCredentials);
      HttpEntity<Void> request = new HttpEntity<>(header);

      response =
          restTemplate.exchange(rocketChatApiRoomsGet, HttpMethod.GET, request, RoomsGetDTO.class);

    } catch (Exception ex) {
      throw new RocketChatGetRoomsException(
          String.format("Could not get Rocket.Chat rooms for user id %s",
              rocketChatCredentials.getRocketChatUserId()));
    }

    if (response.getStatusCode() == HttpStatus.OK) {
      return Arrays.asList(response.getBody().getUpdate());
    } else {
      String error = "Could not get Rocket.Chat rooms for user id %s";
      throw new RocketChatGetRoomsException(
          String.format(error, rocketChatCredentials.getRocketChatUserId()));
    }
  }

  /**
   * Returns the information of the given Rocket.Chat user.
   *
   * @param rcUserId Rocket.Chat user id
   * @throws {@link RocketChatGetUserInfoException}
   * @return the dto containing the user infos
   */
  public UserInfoResponseDTO getUserInfo(String rcUserId) throws RocketChatGetUserInfoException {

    ResponseEntity<UserInfoResponseDTO> response;
    try {
      RocketChatCredentials technicalUser = rcCredentialHelper.getTechnicalUser();
      HttpHeaders header = getStandardHttpHeaders(technicalUser);
      HttpEntity<Void> request = new HttpEntity<>(header);

      response = restTemplate.exchange(rocketChatApiUserInfo + "?userId=" + rcUserId,
          HttpMethod.GET, request, UserInfoResponseDTO.class);

    } catch (Exception ex) {
      throw new RocketChatGetUserInfoException(
          String.format("Could not get Rocket.Chat user info of user id %s", rcUserId), ex);
    }

    if (response.getBody() == null || response.getStatusCode() != HttpStatus.OK || !response
        .getBody().isSuccess()) {
      throw new RocketChatGetUserInfoException(
          String.format("Could not get Rocket.Chat user info of user id %s", rcUserId));
    }

    return response.getBody();
  }

}