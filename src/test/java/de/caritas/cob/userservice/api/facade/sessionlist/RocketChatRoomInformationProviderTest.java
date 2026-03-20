package de.caritas.cob.userservice.api.facade.sessionlist;

import static de.caritas.cob.userservice.api.testHelper.TestConstants.RC_CREDENTIALS;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.RC_CREDENTIALS_WITH_EMPTY_USER_VALUES;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.RC_FEEDBACK_GROUP_ID;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.RC_FEEDBACK_GROUP_ID_2;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.RC_FEEDBACK_GROUP_ID_3;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.RC_GROUP_ID;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.RC_GROUP_ID_2;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.RC_GROUP_ID_3;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.ROOMS_LAST_MESSAGE_DTO_MAP;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.ROOMS_UPDATE_DTO_LIST;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.SUBSCRIPTIONS_UPDATE_LIST_DTO_WITH_ONE_FEEDBACK_UNREAD;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.USERS_ROOMS_LIST;
import static de.caritas.cob.userservice.api.testHelper.TestConstants.USER_DTO_3;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import de.caritas.cob.userservice.api.adapters.rocketchat.RocketChatService;
import de.caritas.cob.userservice.api.adapters.rocketchat.dto.room.RoomsUpdateDTO;
import de.caritas.cob.userservice.api.container.RocketChatRoomInformation;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RocketChatRoomInformationProviderTest {

  @InjectMocks private RocketChatRoomInformationProvider rocketChatRoomInformationProvider;

  @Mock private RocketChatService rocketChatService;

  @Test
  public void retrieveRocketChatInformation_Should_Return_CorrectMessagesReadMap() {

    when(rocketChatService.getSubscriptionsOfUser(RC_CREDENTIALS))
        .thenReturn(SUBSCRIPTIONS_UPDATE_LIST_DTO_WITH_ONE_FEEDBACK_UNREAD);
    RocketChatRoomInformation rocketChatRoomInformation =
        rocketChatRoomInformationProvider.retrieveRocketChatInformation(RC_CREDENTIALS);

    assertTrue(rocketChatRoomInformation.getReadMessages().get(RC_GROUP_ID));
    assertTrue(rocketChatRoomInformation.getReadMessages().get(RC_GROUP_ID_2));
    assertTrue(rocketChatRoomInformation.getReadMessages().get(RC_GROUP_ID_3));
    assertFalse(rocketChatRoomInformation.getReadMessages().get(RC_FEEDBACK_GROUP_ID));
    assertTrue(rocketChatRoomInformation.getReadMessages().get(RC_FEEDBACK_GROUP_ID_2));
    assertTrue(rocketChatRoomInformation.getReadMessages().get(RC_FEEDBACK_GROUP_ID_3));
  }

  @Test
  public void retrieveRocketChatInformation_Should_Return_RocketChatRoomsUpdateList() {

    when(rocketChatService.getRoomsOfUser(RC_CREDENTIALS)).thenReturn(ROOMS_UPDATE_DTO_LIST);
    RocketChatRoomInformation rocketChatRoomInformation =
        rocketChatRoomInformationProvider.retrieveRocketChatInformation(RC_CREDENTIALS);
    assertEquals(ROOMS_UPDATE_DTO_LIST, rocketChatRoomInformation.getRoomsForUpdate());
  }

  @Test
  public void retrieveRocketChatInformation_Should_Return_CorrectRocketChatUserRoomList() {

    when(rocketChatService.getRoomsOfUser(RC_CREDENTIALS)).thenReturn(ROOMS_UPDATE_DTO_LIST);
    RocketChatRoomInformation rocketChatRoomInformation =
        rocketChatRoomInformationProvider.retrieveRocketChatInformation(RC_CREDENTIALS);
    assertEquals(USERS_ROOMS_LIST, rocketChatRoomInformation.getUserRooms());
  }

  @Test
  public void retrieveRocketChatInformation_Should_Return_CorrectRocketChatLastMessageRoom() {

    when(rocketChatService.getRoomsOfUser(RC_CREDENTIALS)).thenReturn(ROOMS_UPDATE_DTO_LIST);
    RocketChatRoomInformation rocketChatRoomInformation =
        rocketChatRoomInformationProvider.retrieveRocketChatInformation(RC_CREDENTIALS);
    assertEquals(ROOMS_LAST_MESSAGE_DTO_MAP, rocketChatRoomInformation.getLastMessagesRoom());
  }

  @Test
  public void retrieveRocketChatInformation_Should_Return_EmptyObject_When_RcUserIdNotSet() {

    RocketChatRoomInformation rocketChatRoomInformation =
        rocketChatRoomInformationProvider.retrieveRocketChatInformation(
            RC_CREDENTIALS_WITH_EMPTY_USER_VALUES);

    assertTrue(Objects.nonNull(rocketChatRoomInformation));
    assertTrue(CollectionUtils.sizeIsEmpty(rocketChatRoomInformation.getReadMessages()));
    assertTrue(CollectionUtils.sizeIsEmpty(rocketChatRoomInformation.getLastMessagesRoom()));
    assertTrue(CollectionUtils.sizeIsEmpty(rocketChatRoomInformation.getRoomsForUpdate()));
  }

  @Test
  public void should_collect_fallback_date_for_rooms_without_last_message() {
    var fallbackDate = new Date(1655730882738L);
    var roomUpdateWithoutLastMessage =
        new RoomsUpdateDTO(
            "4711aaGc",
            "room without last message",
            "fname13",
            "e2e",
            USER_DTO_3,
            true,
            false,
            new Date(),
            null,
            fallbackDate);
    var rooms = new ArrayList<>(ROOMS_UPDATE_DTO_LIST);
    rooms.add(roomUpdateWithoutLastMessage);
    when(rocketChatService.getRoomsOfUser(RC_CREDENTIALS)).thenReturn(rooms);

    RocketChatRoomInformation rocketChatRoomInformation =
        rocketChatRoomInformationProvider.retrieveRocketChatInformation(RC_CREDENTIALS);

    assertNotNull(rocketChatRoomInformation.getGroupIdToLastMessageFallbackDate());
    var fallbackDateOfRoom =
        rocketChatRoomInformation.getGroupIdToLastMessageFallbackDate().get("4711aaGc");
    assertEquals(fallbackDate, fallbackDateOfRoom);
  }

  @Test
  public void should_not_fail_on_rooms_without_fallback_date() {
    var roomUpdateWithoutLastMessage =
        new RoomsUpdateDTO(
            "4711aaGc",
            "room without last message and date",
            "fname13",
            "e2e",
            USER_DTO_3,
            true,
            false,
            new Date(),
            null,
            null);
    var rooms = new ArrayList<>(ROOMS_UPDATE_DTO_LIST);
    rooms.add(roomUpdateWithoutLastMessage);
    when(rocketChatService.getRoomsOfUser(RC_CREDENTIALS)).thenReturn(rooms);

    RocketChatRoomInformation rocketChatRoomInformation =
        rocketChatRoomInformationProvider.retrieveRocketChatInformation(RC_CREDENTIALS);

    assertNotNull(rocketChatRoomInformation.getGroupIdToLastMessageFallbackDate());
    assertTrue(rocketChatRoomInformation.getGroupIdToLastMessageFallbackDate().isEmpty());
  }

  @Test
  public void should_mark_consultant_display_name_changed_room_as_read_when_only_alias_is_unread() {
    var alias =
        new de.caritas.cob.userservice.api.adapters.web.dto.AliasMessageDTO()
            .messageType(
                de.caritas.cob.userservice.api.adapters.web.dto.MessageType
                    .CONSULTANT_DISPLAY_NAME_CHANGED);
    var lastMessage =
        new de.caritas.cob.userservice.api.adapters.rocketchat.dto.room.RoomsLastMessageDTO();
    lastMessage.setAlias(alias);
    lastMessage.setTimestamp(new Date());
    var roomWithAlias =
        new RoomsUpdateDTO(
            "displayNameRoom",
            "room with display name alias",
            "fname",
            "p",
            USER_DTO_3,
            false,
            false,
            new Date(),
            lastMessage,
            new Date());

    // Subscription shows 1 unread (the alias itself)
    var subscriptionWithUnread =
        new de.caritas.cob.userservice.api.adapters.rocketchat.dto.subscriptions
            .SubscriptionsUpdateDTO(
            "B",
            true,
            true,
            1,
            0,
            0,
            new Date(),
            "displayNameRoom",
            "name",
            "fname",
            "p",
            null,
            new Date(1655730882738L),
            new Date(),
            null,
            null);

    var rooms = new ArrayList<>(ROOMS_UPDATE_DTO_LIST);
    rooms.add(roomWithAlias);
    when(rocketChatService.getRoomsOfUser(RC_CREDENTIALS)).thenReturn(rooms);

    var subscriptions = new ArrayList<>(SUBSCRIPTIONS_UPDATE_LIST_DTO_WITH_ONE_FEEDBACK_UNREAD);
    subscriptions.add(subscriptionWithUnread);
    when(rocketChatService.getSubscriptionsOfUser(RC_CREDENTIALS)).thenReturn(subscriptions);

    RocketChatRoomInformation rocketChatRoomInformation =
        rocketChatRoomInformationProvider.retrieveRocketChatInformation(RC_CREDENTIALS);

    // Despite unread == 1 in subscription, the room must be considered read
    assertTrue(rocketChatRoomInformation.getReadMessages().get("displayNameRoom"));
  }

  @Test
  public void
      should_use_last_seen_timestamp_as_fallback_sort_date_for_consultant_display_name_changed_room() {
    var lastSeenDate = new Date(1655730882738L);
    var alias =
        new de.caritas.cob.userservice.api.adapters.web.dto.AliasMessageDTO()
            .messageType(
                de.caritas.cob.userservice.api.adapters.web.dto.MessageType
                    .CONSULTANT_DISPLAY_NAME_CHANGED);
    var lastMessage =
        new de.caritas.cob.userservice.api.adapters.rocketchat.dto.room.RoomsLastMessageDTO();
    lastMessage.setAlias(alias);
    lastMessage.setTimestamp(new Date());
    var roomWithAlias =
        new RoomsUpdateDTO(
            "displayNameRoom2",
            "room",
            "fname",
            "p",
            USER_DTO_3,
            false,
            false,
            new Date(),
            lastMessage,
            new Date());

    var subscriptionWithLastSeen =
        new de.caritas.cob.userservice.api.adapters.rocketchat.dto.subscriptions
            .SubscriptionsUpdateDTO(
            "C",
            true,
            false,
            0,
            0,
            0,
            new Date(),
            "displayNameRoom2",
            "name",
            "fname",
            "p",
            null,
            lastSeenDate,
            new Date(),
            null,
            null);

    var rooms = new ArrayList<>(ROOMS_UPDATE_DTO_LIST);
    rooms.add(roomWithAlias);
    when(rocketChatService.getRoomsOfUser(RC_CREDENTIALS)).thenReturn(rooms);

    var subscriptions = new ArrayList<>(SUBSCRIPTIONS_UPDATE_LIST_DTO_WITH_ONE_FEEDBACK_UNREAD);
    subscriptions.add(subscriptionWithLastSeen);
    when(rocketChatService.getSubscriptionsOfUser(RC_CREDENTIALS)).thenReturn(subscriptions);

    RocketChatRoomInformation rocketChatRoomInformation =
        rocketChatRoomInformationProvider.retrieveRocketChatInformation(RC_CREDENTIALS);

    // The lastSeen timestamp is used as fallback sort date to prevent the alias from
    // pushing the session to the top of the list
    var fallbackDate =
        rocketChatRoomInformation.getGroupIdToLastMessageFallbackDate().get("displayNameRoom2");
    assertEquals(lastSeenDate, fallbackDate);
  }

  @Test
  public void
      should_not_mark_as_read_and_not_override_sort_date_when_real_unread_messages_exist_alongside_display_name_changed() {
    var alias =
        new de.caritas.cob.userservice.api.adapters.web.dto.AliasMessageDTO()
            .messageType(
                de.caritas.cob.userservice.api.adapters.web.dto.MessageType
                    .CONSULTANT_DISPLAY_NAME_CHANGED);
    var lastMessage =
        new de.caritas.cob.userservice.api.adapters.rocketchat.dto.room.RoomsLastMessageDTO();
    lastMessage.setAlias(alias);
    lastMessage.setTimestamp(new Date());
    var roomWithAlias =
        new RoomsUpdateDTO(
            "roomWithRealUnread",
            "room",
            "fname",
            "p",
            USER_DTO_3,
            false,
            false,
            new Date(),
            lastMessage,
            new Date());

    // unread = 3: the alias + 2 real messages that the user hasn't read yet
    var subscriptionWithMultipleUnread =
        new de.caritas.cob.userservice.api.adapters.rocketchat.dto.subscriptions
            .SubscriptionsUpdateDTO(
            "D",
            true,
            true,
            3,
            0,
            0,
            new Date(),
            "roomWithRealUnread",
            "name",
            "fname",
            "p",
            null,
            new Date(1655730882738L),
            new Date(),
            null,
            null);

    var rooms = new ArrayList<>(ROOMS_UPDATE_DTO_LIST);
    rooms.add(roomWithAlias);
    when(rocketChatService.getRoomsOfUser(RC_CREDENTIALS)).thenReturn(rooms);

    var subscriptions = new ArrayList<>(SUBSCRIPTIONS_UPDATE_LIST_DTO_WITH_ONE_FEEDBACK_UNREAD);
    subscriptions.add(subscriptionWithMultipleUnread);
    when(rocketChatService.getSubscriptionsOfUser(RC_CREDENTIALS)).thenReturn(subscriptions);

    RocketChatRoomInformation rocketChatRoomInformation =
        rocketChatRoomInformationProvider.retrieveRocketChatInformation(RC_CREDENTIALS);

    // room has real unread messages → must stay unread
    assertFalse(rocketChatRoomInformation.getReadMessages().get("roomWithRealUnread"));
    // sort date must NOT be overridden → alias timestamp drives sort position
    assertFalse(
        rocketChatRoomInformation
            .getGroupIdToLastMessageFallbackDate()
            .containsKey("roomWithRealUnread"));
  }
}
