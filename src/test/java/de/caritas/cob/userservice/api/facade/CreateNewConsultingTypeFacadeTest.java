package de.caritas.cob.userservice.api.facade;

import static de.caritas.cob.userservice.testHelper.TestConstants.CONSULTING_TYPE_ID_KREUZBUND;
import static de.caritas.cob.userservice.testHelper.TestConstants.CONSULTING_TYPE_ID_SUCHT;
import static de.caritas.cob.userservice.testHelper.TestConstants.CONSULTING_TYPE_SETTINGS_KREUZBUND;
import static de.caritas.cob.userservice.testHelper.TestConstants.CONSULTING_TYPE_SETTINGS_SUCHT;
import static de.caritas.cob.userservice.testHelper.TestConstants.INVALID_CONSULTING_TYPE_ID;
import static de.caritas.cob.userservice.testHelper.TestConstants.UNKNOWN_CONSULTING_TYPE_ID;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.caritas.cob.userservice.api.container.RocketChatCredentials;
import de.caritas.cob.userservice.api.exception.MissingConsultingTypeException;
import de.caritas.cob.userservice.api.exception.httpresponses.BadRequestException;
import de.caritas.cob.userservice.api.manager.consultingtype.ConsultingTypeManager;
import de.caritas.cob.userservice.api.model.registration.UserDTO;
import de.caritas.cob.userservice.api.repository.user.User;
import de.caritas.cob.userservice.consultingtypeservice.generated.web.model.ExtendedConsultingTypeResponseDTO;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CreateNewConsultingTypeFacadeTest {

  @InjectMocks
  private CreateNewConsultingTypeFacade createNewConsultingTypeFacade;
  @Mock
  private ConsultingTypeManager consultingTypeManager;
  @Mock
  private CreateUserChatRelationFacade createUserChatRelationFacade;
  @Mock
  private CreateSessionFacade createSessionFacade;

  @Test
  public void initializeNewConsultingType_Should_RegisterNewSession_When_ProvidedWithSessionConsultingType_For_NewAccountRegistrations() {
    EasyRandom easyRandom = new EasyRandom();
    UserDTO userDTO = easyRandom.nextObject(UserDTO.class);
    userDTO.setConsultingType(String.valueOf(0));
    User user = easyRandom.nextObject(User.class);
    ExtendedConsultingTypeResponseDTO extendedConsultingTypeResponseDTO = easyRandom
        .nextObject(ExtendedConsultingTypeResponseDTO.class);
    extendedConsultingTypeResponseDTO.setId(0);

    createNewConsultingTypeFacade
        .initializeNewConsultingType(userDTO, user, extendedConsultingTypeResponseDTO);
    if (!extendedConsultingTypeResponseDTO.getGroupChat().getIsGroupChat()) {
      verify(createSessionFacade, times(1)).createUserSession(any(), any(), any());
    } else {
      verify(createUserChatRelationFacade, times(1))
          .initializeUserChatAgencyRelation(any(), any(), any());
    }

  }

  @Test
  public void initializeNewConsultingType_Should_RegisterNewSessionAndReturnSessionId_When_ProvidedWithSessionConsultingType_For_NewConsultingTypeRegistrations() {
    EasyRandom easyRandom = new EasyRandom();
    UserDTO userDTO = easyRandom.nextObject(UserDTO.class);
    userDTO.setConsultingType(String.valueOf(CONSULTING_TYPE_ID_SUCHT));
    User user = easyRandom.nextObject(User.class);
    RocketChatCredentials rocketChatCredentials = easyRandom
        .nextObject(RocketChatCredentials.class);

    when(createSessionFacade.createUserSession(any(), any(), any())).thenReturn(1L);
    when(consultingTypeManager.getConsultingTypeSettings("0"))
        .thenReturn(CONSULTING_TYPE_SETTINGS_SUCHT);

    long sessionId = createNewConsultingTypeFacade
        .initializeNewConsultingType(userDTO, user, rocketChatCredentials);

    assertEquals(1L, sessionId);
    verify(createSessionFacade, times(1)).createUserSession(any(), any(), any());
  }

  @Test
  public void initializeNewConsultingType_Should_RegisterNewUserChatRelationAndReturnNull_When_ProvidedWithGroupChatConsultingType_For_NewConsultingTypeRegistrations() {
    EasyRandom easyRandom = new EasyRandom();
    UserDTO userDTO = easyRandom.nextObject(UserDTO.class);
    userDTO.setConsultingType(String.valueOf(CONSULTING_TYPE_ID_KREUZBUND));
    User user = easyRandom.nextObject(User.class);
    RocketChatCredentials rocketChatCredentials = easyRandom
        .nextObject(RocketChatCredentials.class);

    when(consultingTypeManager.getConsultingTypeSettings("15"))
        .thenReturn(CONSULTING_TYPE_SETTINGS_KREUZBUND);

    Long sessionId = createNewConsultingTypeFacade
        .initializeNewConsultingType(userDTO, user, rocketChatCredentials);

    assertEquals(null, sessionId);
    verify(createUserChatRelationFacade, times(1))
        .initializeUserChatAgencyRelation(any(), any(), any());
  }

  @Test(expected = BadRequestException.class)
  public void initializeNewConsultingType_Should_ThrowBadRequestException_When_ProvidedWithInvalidConsultingType_For_NewConsultingTypeRegistrations() {
    EasyRandom easyRandom = new EasyRandom();
    UserDTO userDTO = easyRandom.nextObject(UserDTO.class);
    userDTO.setConsultingType(INVALID_CONSULTING_TYPE_ID);
    User user = easyRandom.nextObject(User.class);
    RocketChatCredentials rocketChatCredentials = easyRandom
        .nextObject(RocketChatCredentials.class);
    when(consultingTypeManager.getConsultingTypeSettings(INVALID_CONSULTING_TYPE_ID))
        .thenThrow(new NumberFormatException(""));
    createNewConsultingTypeFacade.initializeNewConsultingType(userDTO, user, rocketChatCredentials);

    verify(createUserChatRelationFacade, times(0))
        .initializeUserChatAgencyRelation(any(), any(), any());
  }

  @Test(expected = BadRequestException.class)
  public void initializeNewConsultingType_Should_ThrowBadRequestException_When_ProvidedWithUnknownConsultingType_For_NewConsultingTypeRegistrations() {
    EasyRandom easyRandom = new EasyRandom();
    UserDTO userDTO = easyRandom.nextObject(UserDTO.class);
    userDTO.setConsultingType(UNKNOWN_CONSULTING_TYPE_ID);
    User user = easyRandom.nextObject(User.class);
    RocketChatCredentials rocketChatCredentials = easyRandom
        .nextObject(RocketChatCredentials.class);
    when(consultingTypeManager.getConsultingTypeSettings(UNKNOWN_CONSULTING_TYPE_ID)).thenThrow(
        new MissingConsultingTypeException(""));
    createNewConsultingTypeFacade.initializeNewConsultingType(userDTO, user, rocketChatCredentials);

    verify(createUserChatRelationFacade, times(0))
        .initializeUserChatAgencyRelation(any(), any(), any());
  }
}
