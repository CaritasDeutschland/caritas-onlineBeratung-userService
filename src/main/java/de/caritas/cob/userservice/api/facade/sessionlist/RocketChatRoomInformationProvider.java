package de.caritas.cob.userservice.api.facade.sessionlist;

import static de.caritas.cob.userservice.api.adapters.web.dto.MessageType.CONSULTANT_DISPLAY_NAME_CHANGED;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import de.caritas.cob.userservice.api.adapters.rocketchat.RocketChatCredentials;
import de.caritas.cob.userservice.api.adapters.rocketchat.RocketChatService;
import de.caritas.cob.userservice.api.adapters.rocketchat.dto.room.RoomsLastMessageDTO;
import de.caritas.cob.userservice.api.adapters.rocketchat.dto.room.RoomsUpdateDTO;
import de.caritas.cob.userservice.api.adapters.rocketchat.dto.subscriptions.SubscriptionsUpdateDTO;
import de.caritas.cob.userservice.api.container.RocketChatRoomInformation;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RocketChatRoomInformationProvider {

  private final RocketChatService rocketChatService;

  @Autowired
  public RocketChatRoomInformationProvider(RocketChatService rocketChatService) {
    this.rocketChatService = requireNonNull(rocketChatService);
  }

  /**
   * Get room and update information from Rocket.Chat for a user.
   *
   * @param rocketChatCredentials the Rocket.Chat credentials of the user
   * @return an instance of {@link RocketChatRoomInformation}
   */
  public RocketChatRoomInformation retrieveRocketChatInformation(
      RocketChatCredentials rocketChatCredentials) {

    Map<String, Boolean> readMessages = emptyMap();
    List<RoomsUpdateDTO> roomsForUpdate = emptyList();
    Map<String, Date> displayNameChangedFallbackDates = emptyMap();

    if (nonNull(rocketChatCredentials.getRocketChatUserId())) {
      var subscriptions = rocketChatService.getSubscriptionsOfUser(rocketChatCredentials);
      readMessages = buildMessagesWithReadInfo(subscriptions);
      roomsForUpdate = rocketChatService.getRoomsOfUser(rocketChatCredentials);

      var displayNameChangedRoomIds = collectDisplayNameChangedRoomIds(roomsForUpdate);
      if (!displayNameChangedRoomIds.isEmpty()) {
        readMessages =
            overrideReadStatusForDisplayNameChangedRooms(readMessages, displayNameChangedRoomIds);
        displayNameChangedFallbackDates =
            collectLastSeenDatesForDisplayNameChangedRooms(
                subscriptions, displayNameChangedRoomIds);
      }
    }

    var userRooms = roomsForUpdate.stream().map(RoomsUpdateDTO::getId).collect(Collectors.toList());
    var lastMessagesRoom = getRcRoomLastMessages(roomsForUpdate);
    var groupIdToLastMessageFallbackDate =
        collectFallbackDateOfRoomsWithoutLastMessage(roomsForUpdate);

    var mergedFallbackDates = new HashMap<>(groupIdToLastMessageFallbackDate);
    mergedFallbackDates.putAll(displayNameChangedFallbackDates);

    return RocketChatRoomInformation.builder()
        .readMessages(readMessages)
        .roomsForUpdate(roomsForUpdate)
        .userRooms(userRooms)
        .lastMessagesRoom(lastMessagesRoom)
        .groupIdToLastMessageFallbackDate(mergedFallbackDates)
        .build();
  }

  private Map<String, Boolean> buildMessagesWithReadInfo(
      List<SubscriptionsUpdateDTO> subscriptions) {
    return subscriptions.stream()
        .collect(Collectors.toMap(SubscriptionsUpdateDTO::getRoomId, this::isMessageRead));
  }

  /**
   * Collects the IDs of rooms whose last message is a {@code CONSULTANT_DISPLAY_NAME_CHANGED}
   * alias. These are pure system notifications that should neither affect the sort order nor the
   * unread-message indicator of a session.
   */
  private Set<String> collectDisplayNameChangedRoomIds(List<RoomsUpdateDTO> rooms) {
    return rooms.stream()
        .filter(room -> isConsultantDisplayNameChangedAlias(room.getLastMessage()))
        .map(RoomsUpdateDTO::getId)
        .collect(Collectors.toSet());
  }

  /**
   * Overrides the read-status for all {@code CONSULTANT_DISPLAY_NAME_CHANGED} rooms to {@code true}
   * so that the unread indicator is not shown for those sessions.
   */
  private Map<String, Boolean> overrideReadStatusForDisplayNameChangedRooms(
      Map<String, Boolean> readMessages, Set<String> displayNameChangedRoomIds) {
    var result = new HashMap<>(readMessages);
    displayNameChangedRoomIds.forEach(roomId -> result.put(roomId, true));
    return result;
  }

  /**
   * Builds a roomId → lastSeenTimestamp map for {@code CONSULTANT_DISPLAY_NAME_CHANGED} rooms. The
   * subscription's {@code ls} (lastSeen) timestamp represents when the user last actually read the
   * room – i.e. before the display-name alias was posted. Using it as the sort fallback keeps the
   * session at its original position in the list.
   */
  private Map<String, Date> collectLastSeenDatesForDisplayNameChangedRooms(
      List<SubscriptionsUpdateDTO> subscriptions, Set<String> displayNameChangedRoomIds) {
    return subscriptions.stream()
        .filter(sub -> displayNameChangedRoomIds.contains(sub.getRoomId()))
        .filter(sub -> nonNull(sub.getLastSeenTimestamp()))
        .collect(
            Collectors.toMap(
                SubscriptionsUpdateDTO::getRoomId, SubscriptionsUpdateDTO::getLastSeenTimestamp));
  }

  private boolean isConsultantDisplayNameChangedAlias(RoomsLastMessageDTO lastMessage) {
    if (isNull(lastMessage) || isNull(lastMessage.getAlias())) {
      return false;
    }
    var alias = lastMessage.getAlias();
    return nonNull(alias.getMessageType())
        && CONSULTANT_DISPLAY_NAME_CHANGED.name().equals(alias.getMessageType().name());
  }

  private boolean isMessageRead(SubscriptionsUpdateDTO subscription) {
    return nonNull(subscription.getUnread()) && subscription.getUnread() == 0;
  }

  private Map<String, RoomsLastMessageDTO> getRcRoomLastMessages(
      List<RoomsUpdateDTO> roomsUpdateList) {

    return roomsUpdateList.stream()
        .filter(this::isLastMessageAndTimestampForRocketChatRoomAvailable)
        .collect(Collectors.toMap(RoomsUpdateDTO::getId, RoomsUpdateDTO::getLastMessage));
  }

  private boolean isLastMessageAndTimestampForRocketChatRoomAvailable(RoomsUpdateDTO room) {
    return nonNull(room.getLastMessage()) && nonNull(room.getLastMessage().getTimestamp());
  }

  private Map<String, Date> collectFallbackDateOfRoomsWithoutLastMessage(
      List<RoomsUpdateDTO> roomsForUpdate) {
    return roomsForUpdate.stream()
        .filter(room -> !isLastMessageAndTimestampForRocketChatRoomAvailable(room))
        .filter(room -> nonNull(room.getLastMessageDate()))
        .collect(Collectors.toMap(RoomsUpdateDTO::getId, RoomsUpdateDTO::getLastMessageDate));
  }
}
