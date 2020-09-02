package de.caritas.cob.userservice.api.facade;

import de.caritas.cob.userservice.api.authorization.Authority;
import de.caritas.cob.userservice.api.container.CreateEnquiryExceptionInformation;
import de.caritas.cob.userservice.api.container.RocketChatCredentials;
import de.caritas.cob.userservice.api.exception.CheckForCorrectRocketChatUserException;
import de.caritas.cob.userservice.api.exception.CreateMonitoringException;
import de.caritas.cob.userservice.api.exception.EnquiryMessageException;
import de.caritas.cob.userservice.api.exception.InitializeFeedbackChatException;
import de.caritas.cob.userservice.api.exception.MessageHasAlreadyBeenSavedException;
import de.caritas.cob.userservice.api.exception.NoUserSessionException;
import de.caritas.cob.userservice.api.exception.SaveUserException;
import de.caritas.cob.userservice.api.exception.UpdateFeedbackGroupIdException;
import de.caritas.cob.userservice.api.exception.httpresponses.BadRequestException;
import de.caritas.cob.userservice.api.exception.httpresponses.ConflictException;
import de.caritas.cob.userservice.api.exception.httpresponses.InternalServerErrorException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatAddConsultantsAndTechUserException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatAddUserToGroupException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatCreateGroupException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatGetUserInfoException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatPostMessageException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatPostWelcomeMessageException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatRemoveSystemMessagesException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatUserNotInitializedException;
import de.caritas.cob.userservice.api.helper.Helper;
import de.caritas.cob.userservice.api.helper.RocketChatHelper;
import de.caritas.cob.userservice.api.helper.UserHelper;
import de.caritas.cob.userservice.api.manager.consultingType.ConsultingTypeManager;
import de.caritas.cob.userservice.api.manager.consultingType.ConsultingTypeSettings;
import de.caritas.cob.userservice.api.model.rocketChat.group.GroupResponseDTO;
import de.caritas.cob.userservice.api.model.rocketChat.user.UserInfoResponseDTO;
import de.caritas.cob.userservice.api.repository.consultantAgency.ConsultantAgency;
import de.caritas.cob.userservice.api.repository.session.Session;
import de.caritas.cob.userservice.api.repository.session.SessionStatus;
import de.caritas.cob.userservice.api.repository.user.User;
import de.caritas.cob.userservice.api.service.ConsultantAgencyService;
import de.caritas.cob.userservice.api.service.LogService;
import de.caritas.cob.userservice.api.service.MonitoringService;
import de.caritas.cob.userservice.api.service.RocketChatService;
import de.caritas.cob.userservice.api.service.SessionService;
import de.caritas.cob.userservice.api.service.helper.KeycloakAdminClientHelper;
import de.caritas.cob.userservice.api.service.helper.MessageServiceHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/*
 * Facade for capsuling the steps for saving the enquiry message.
 */
@Service
public class CreateEnquiryMessageFacade {

  @Value("${rocket.systemuser.id}")
  private String ROCKET_CHAT_SYSTEM_USER_ID;

  private final SessionService sessionService;
  private final RocketChatService rocketChatService;
  private final EmailNotificationFacade emailNotificationFacade;
  private final MessageServiceHelper messageServiceHelper;
  private final ConsultantAgencyService consultantAgencyService;
  private final MonitoringService monitoringService;
  private final ConsultingTypeManager consultingTypeManager;
  private final KeycloakAdminClientHelper keycloakAdminClientHelper;
  private final UserHelper userHelper;
  private final RocketChatHelper rocketChatHelper;

  /**
   * Constructor
   */
  @Autowired
  public CreateEnquiryMessageFacade(SessionService sessionService,
      RocketChatService rocketChatService, EmailNotificationFacade emailNotificationFacade,
      MessageServiceHelper messageServiceHelper, ConsultantAgencyService consultantAgencyService,
      MonitoringService monitoringService, ConsultingTypeManager consultingTypeManager,
      KeycloakAdminClientHelper keycloakHelper, UserHelper userHelper,
      RocketChatHelper rocketChatHelper) {
    this.sessionService = sessionService;
    this.rocketChatService = rocketChatService;
    this.emailNotificationFacade = emailNotificationFacade;
    this.messageServiceHelper = messageServiceHelper;
    this.consultantAgencyService = consultantAgencyService;
    this.monitoringService = monitoringService;
    this.consultingTypeManager = consultingTypeManager;
    this.keycloakAdminClientHelper = keycloakHelper;
    this.userHelper = userHelper;
    this.rocketChatHelper = rocketChatHelper;
  }

  /**
   * Handles possible exceptions and roll backs during the creation of the enquiry message for a
   * session.
   *
   * @param user {@link User}
   * @param sessionId {@link Session#getId()}
   * @param message enquiry message
   * @param rocketChatCredentials {@link RocketChatCredentials}
   */
  public void createEnquiryMessage(User user, Long sessionId, String message,
      RocketChatCredentials rocketChatCredentials) {

    try {
      doCreateEnquiryMessageSteps(user, sessionId, message, rocketChatCredentials);

    } catch (MessageHasAlreadyBeenSavedException messageHasAlreadyBeenSavedException) {
      throw new ConflictException(messageHasAlreadyBeenSavedException.getMessage(),
          LogService::logCreateEnquiryMessageException);
    } catch (NoUserSessionException
        | CheckForCorrectRocketChatUserException checkForCorrectRocketChatUserException) {
      throw new BadRequestException(checkForCorrectRocketChatUserException.getMessage(),
          LogService::logCreateEnquiryMessageException);
    } catch (RocketChatCreateGroupException | RocketChatGetUserInfoException exception) {
      throw new InternalServerErrorException(exception.getMessage(),
          LogService::logCreateEnquiryMessageException);
    } catch (RocketChatAddConsultantsAndTechUserException | CreateMonitoringException
        | RocketChatPostMessageException | RocketChatPostWelcomeMessageException
        | EnquiryMessageException | InitializeFeedbackChatException exception) {
      doRollback(exception.getExceptionInformation(), rocketChatCredentials);
      throw new InternalServerErrorException(exception.getMessage(),
          LogService::logCreateEnquiryMessageException);
    } catch (InternalServerErrorException | SaveUserException exception) {
      // Presumably only the Rocket.Chat group was created yet
      Optional<Session> session = sessionService.getSession(sessionId);
      CreateEnquiryExceptionInformation exceptionInformation =
          CreateEnquiryExceptionInformation.builder().rcGroupId(session.get().getGroupId()).build();
      doRollback(exceptionInformation, rocketChatCredentials);

      throw new InternalServerErrorException(exception.getMessage(),
          LogService::logCreateEnquiryMessageException);
    }
  }

  /**
   * Creates the private Rocket.Chat group, initializes the session monitoring and saves the enquiry
   * message in Rocket.Chat.
   *
   * @param user {@link User}
   * @param sessionId {@link Session#getId()}
   * @param message Message
   * @param rocketChatCredentials {@link RocketChatCredentials}
   */
  private void doCreateEnquiryMessageSteps(User user, Long sessionId, String message,
      RocketChatCredentials rocketChatCredentials)
      throws RocketChatAddConsultantsAndTechUserException, CreateMonitoringException,
      RocketChatPostMessageException, RocketChatPostWelcomeMessageException,
      EnquiryMessageException, InitializeFeedbackChatException, RocketChatCreateGroupException,
      RocketChatGetUserInfoException, SaveUserException {

    Session session = getSessionForEnquiryMessage(sessionId, user);

    checkIfKeycloakAndRocketChatUsernamesMatch(rocketChatCredentials.getRocketChatUserId(), user);

    GroupResponseDTO rcGroupDTO = createRocketChatGroupForSession(session, rocketChatCredentials);
    userHelper.updateRocketChatIdInDatabase(user, rcGroupDTO.getGroup().getUser().getId());

    List<ConsultantAgency> agencyList =
        consultantAgencyService.findConsultantsByAgencyId(session.getAgencyId());

    addConsultantsAndTechUserToGroup(rcGroupDTO.getGroup().getId(), rocketChatCredentials,
        agencyList);

    ConsultingTypeSettings consultingTypeSettings =
        consultingTypeManager.getConsultantTypeSettings(session.getConsultingType());
    monitoringService.createMonitoring(session, consultingTypeSettings);

    CreateEnquiryExceptionInformation createEnquiryExceptionInformation =
        CreateEnquiryExceptionInformation.builder().session(session)
            .rcGroupId(rcGroupDTO.getGroup().getId()).build();
    messageServiceHelper.postMessage(message, rocketChatCredentials, rcGroupDTO.getGroup().getId(),
        createEnquiryExceptionInformation);

    sessionService.saveEnquiryMessageDateAndRocketChatGroupId(session,
        rcGroupDTO.getGroup().getId());

    messageServiceHelper.postWelcomeMessage(rcGroupDTO.getGroup().getId(), user,
        consultingTypeSettings, createEnquiryExceptionInformation);

    initializeFeedbackChat(session, rcGroupDTO.getGroup().getId(), agencyList,
        consultingTypeSettings);

    emailNotificationFacade.sendNewEnquiryEmailNotification(session);
  }

  private Session getSessionForEnquiryMessage(Long sessionId, User user)
      throws NoUserSessionException, MessageHasAlreadyBeenSavedException {

    Optional<Session> session;

    session = sessionService.getSession(sessionId);

    if (!session.isPresent() || !session.get().getUser().getUserId().equals(user.getUserId())) {
      throw new NoUserSessionException(
          String.format("Session %s not found for user %s", sessionId, user.getUserId()));
    }

    if (session.get().getEnquiryMessageDate() != null) {
      throw new MessageHasAlreadyBeenSavedException(
          String.format("Enquiry message already written for session %s", sessionId));
    }

    return session.get();
  }

  private void checkIfKeycloakAndRocketChatUsernamesMatch(String rcUserId, User user)
      throws RocketChatGetUserInfoException {

    UserInfoResponseDTO rcUserInfoDTO = rocketChatService.getUserInfo(rcUserId);
    if (!userHelper.doUsernamesMatch(rcUserInfoDTO.getUser().getUsername(), user.getUsername())) {
      throw new CheckForCorrectRocketChatUserException(String.format(
          "Enquiry message check: User with username %s does not match user with Rocket.Chat ID %s",
          user.getUsername(), rcUserId));
    }
  }

  private GroupResponseDTO createRocketChatGroupForSession(Session session,
      RocketChatCredentials rocketChatCredentials) throws RocketChatCreateGroupException {

    Optional<GroupResponseDTO> rcGroupDTO = rocketChatService
        .createPrivateGroup(rocketChatHelper.generateGroupName(session), rocketChatCredentials);

    if (!rcGroupDTO.isPresent()) {
      throw new RocketChatCreateGroupException(
          String.format("Could not create Rocket.Chat room for session %s and Rocket.Chat user %s",
              session.getId(), rocketChatCredentials.getRocketChatUserId()));
    }

    return rcGroupDTO.get();
  }

  private void initializeFeedbackChat(Session session, String rcGroupId,
      List<ConsultantAgency> agencyList, ConsultingTypeSettings consultingTypeSettings)
      throws InitializeFeedbackChatException {

    if (!consultingTypeSettings.isFeedbackChat()) {
      return;
    }

    String rcFeedbackGroupId = null;
    Optional<GroupResponseDTO> rcFeedbackGroupDTO = Optional.empty();
    CreateEnquiryExceptionInformation exceptionWithoutFeedbackId =
        CreateEnquiryExceptionInformation.builder().session(session).rcGroupId(rcGroupId).build();

    try {
      rcFeedbackGroupDTO = rocketChatService
          .createPrivateGroupWithSystemUser(rocketChatHelper.generateFeedbackGroupName(session));

      if (!rcFeedbackGroupDTO.isPresent() || rcFeedbackGroupDTO.get().getGroup().getId() == null) {
        throw new InitializeFeedbackChatException(
            String.format("Could not create feedback chat group for session %s", session.getId()),
            exceptionWithoutFeedbackId);
      }
      rcFeedbackGroupId = rcFeedbackGroupDTO.get().getGroup().getId();

      // Add RocketChat user for system message to group
      rocketChatService.addUserToGroup(ROCKET_CHAT_SYSTEM_USER_ID, rcFeedbackGroupId);

      // Add all consultants of the session's agency to the feedback group that have the right
      // to view all feedback sessions
      for (ConsultantAgency agency : agencyList) {
        if (keycloakAdminClientHelper.userHasAuthority(agency.getConsultant().getId(),
            Authority.VIEW_ALL_FEEDBACK_SESSIONS)) {
          rocketChatService.addUserToGroup(agency.getConsultant().getRocketChatId(),
              rcFeedbackGroupId);
        }
      }

      // Remove all system messages
      rocketChatService.removeSystemMessages(rcFeedbackGroupId,
          LocalDateTime.now().minusHours(Helper.ONE_DAY_IN_HOURS), LocalDateTime.now());

      // Update the session's feedback group id
      sessionService.updateFeedbackGroupId(Optional.of(session), rcFeedbackGroupId);

    } catch (RocketChatCreateGroupException rocketChatCreateGroupException) {
      throw new InitializeFeedbackChatException(
          String.format("Could not create feedback chat group for session %s", session.getId()),
          exceptionWithoutFeedbackId);

    } catch (RocketChatAddUserToGroupException | RocketChatRemoveSystemMessagesException
        | UpdateFeedbackGroupIdException | RocketChatUserNotInitializedException exception) {
      CreateEnquiryExceptionInformation exceptionWithFeedbackId =
          CreateEnquiryExceptionInformation.builder().session(session).rcGroupId(rcGroupId)
              .rcFeedbackGroupId(rcFeedbackGroupId).build();
      throw new InitializeFeedbackChatException(
          String.format("Could not create feedback chat group for session %s", session.getId()),
          exceptionWithFeedbackId);
    }
  }

  private void addConsultantsAndTechUserToGroup(String rcGroupId,
      RocketChatCredentials rocketChatCredentials, List<ConsultantAgency> agencyList)
      throws RocketChatAddConsultantsAndTechUserException {

    try {
      // Add RocketChat user for system message to group
      rocketChatService.addUserToGroup(ROCKET_CHAT_SYSTEM_USER_ID, rcGroupId);

      if (agencyList != null) {
        for (ConsultantAgency agency : agencyList) {
          rocketChatService.addUserToGroup(agency.getConsultant().getRocketChatId(), rcGroupId);
        }
      }

    } catch (RocketChatAddUserToGroupException rocketChatAddUserToGroupException) {
      throw new RocketChatAddConsultantsAndTechUserException(
          String.format(
              "Add consultants and tech user error: Could not add user with ID %s to group %s",
              rocketChatCredentials.getRocketChatUserId(), rcGroupId),
          rocketChatAddUserToGroupException,
          CreateEnquiryExceptionInformation.builder().rcGroupId(rcGroupId).build());
    }
  }

  private void doRollback(CreateEnquiryExceptionInformation createEnquiryExceptionInformation,
      RocketChatCredentials rocketChatCredentials) {

    if (createEnquiryExceptionInformation == null) {
      return;
    }

    if (createEnquiryExceptionInformation.getRcGroupId() != null) {
      rollbackCreateGroup(createEnquiryExceptionInformation.getRcGroupId(), rocketChatCredentials);
    }
    if (createEnquiryExceptionInformation.getSession() != null) {
      monitoringService
          .rollbackInitializeMonitoring(createEnquiryExceptionInformation.getSession());
      if (createEnquiryExceptionInformation.getRcFeedbackGroupId() != null) {
        rollbackCreateGroup(createEnquiryExceptionInformation.getRcFeedbackGroupId(),
            rocketChatCredentials);
        rollbackSession(createEnquiryExceptionInformation.getSession());
      }
    }
  }

  private void rollbackCreateGroup(String rcGroupId, RocketChatCredentials rocketChatCredentials) {
    if (rcGroupId != null) {
      if (!rocketChatService.rollbackGroup(rcGroupId, rocketChatCredentials)) {
        LogService.logInternalServerError(String.format(
            "Error during rollback while saving enquiry message. Group with id %s could not be deleted.",
            rcGroupId));
      }
    }
  }

  private void rollbackSession(Session session) {
    if (session != null) {

      try {
        session.setEnquiryMessageDate(null);
        session.setStatus(SessionStatus.INITIAL);
        session.setGroupId(null);
        session.setFeedbackGroupId(null);
        sessionService.saveSession(session);
      } catch (InternalServerErrorException ex) {
        LogService.logInternalServerError(String.format(
            "Error during rollback while saving session. Session data could not be set to state before createEnquiryMessageFacade.",
            session.getId()), ex);
      }
    }
  }
}