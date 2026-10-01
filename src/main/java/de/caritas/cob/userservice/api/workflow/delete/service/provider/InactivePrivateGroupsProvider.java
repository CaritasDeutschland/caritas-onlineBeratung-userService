package de.caritas.cob.userservice.api.workflow.delete.service.provider;

import de.caritas.cob.userservice.api.adapters.rocketchat.RocketChatService;
import de.caritas.cob.userservice.api.adapters.rocketchat.dto.group.GroupDTO;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatGetGroupsListAllException;
import de.caritas.cob.userservice.api.helper.CustomLocalDateTime;
import de.caritas.cob.userservice.api.model.Chat;
import de.caritas.cob.userservice.api.model.Session;
import de.caritas.cob.userservice.api.port.out.ChatRepository;
import de.caritas.cob.userservice.api.port.out.SessionRepository;
import de.caritas.cob.userservice.api.service.LogService;
import de.caritas.cob.userservice.api.workflow.delete.model.InactiveGroup;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Provider for user to inactive Rocket.Chat group map. */
@Service
@RequiredArgsConstructor
@Slf4j
public class InactivePrivateGroupsProvider {

  private final @NonNull RocketChatService rocketChatService;
  private final @NonNull ChatRepository chatRepository;
  private final @NonNull SessionRepository sessionRepository;

  /** Chunk size for the {@code IN (...)} lookup of session group ids. */
  private static final int GROUP_ID_LOOKUP_CHUNK_SIZE = 1000;

  @Value("${session.inactive.deleteWorkflow.check.days}")
  private int sessionInactiveDeleteWorkflowCheckDays;

  /**
   * Get a map with users and their related inactive Rocket.Chat group info including last message
   * timestamp. Group chats and feedback rooms are excluded.
   *
   * <p>Feedback rooms must never be processed on their own: Rocket.Chat only reports "a private
   * group whose last message is old", and a feedback room is frequently quiet even while the
   * session it belongs to is actively used. Deleting it would leave {@code
   * session.rc_feedback_group_id} pointing at a room that no longer exists, which in turn breaks
   * de-archiving of that session (see CARITAS-1038). Feedback rooms are deleted together with their
   * session in {@code DeleteRoomsAndSessionAction}.
   *
   * @return a map with users and related inactive Rocket.Chat group info
   */
  public Map<String, List<InactiveGroup>> retrieveUserWithInactiveGroupInfoMap() {

    List<GroupDTO> inactiveGroups = fetchAllInactivePrivateGroups();

    Set<String> groupChatIdSet = buildSetOfGroupChatGroupIds();
    Set<String> feedbackGroupIdSet = buildSetOfFeedbackGroupIds(inactiveGroups);

    Map<String, List<InactiveGroup>> userWithInactiveGroupsMap = new HashMap<>();
    inactiveGroups.stream()
        .filter(group -> isDeletableGroup(group, groupChatIdSet, feedbackGroupIdSet))
        .forEach(
            group ->
                userWithInactiveGroupsMap
                    .computeIfAbsent(group.getUser().getId(), v -> new ArrayList<>())
                    .add(
                        InactiveGroup.builder()
                            .groupId(group.getId())
                            .lastMessageDate(group.getLastMessageDate())
                            .build()));
    return userWithInactiveGroupsMap;
  }

  private boolean isDeletableGroup(
      GroupDTO group, Set<String> groupChatIdSet, Set<String> feedbackGroupIdSet) {
    if (groupChatIdSet.contains(group.getId())) {
      log.info("Inactive group with id {} is skipped, because it is a group chat.", group.getId());
      return false;
    }
    if (feedbackGroupIdSet.contains(group.getId())) {
      log.info(
          "Inactive group with id {} is skipped, because it is the feedback room of a session. "
              + "Feedback rooms are only deleted together with their session.",
          group.getId());
      return false;
    }
    return true;
  }

  private Set<String> buildSetOfGroupChatGroupIds() {
    List<Chat> chatList = IterableUtils.toList(chatRepository.findAll());
    return chatList.stream().map(Chat::getGroupId).collect(Collectors.toSet());
  }

  /**
   * Collects, out of the given inactive groups, those ids that a session references as its feedback
   * room. Queried in chunks to keep the {@code IN (...)} clause within database limits.
   */
  private Set<String> buildSetOfFeedbackGroupIds(List<GroupDTO> inactiveGroups) {
    List<String> inactiveGroupIds =
        inactiveGroups.stream()
            .map(GroupDTO::getId)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .collect(Collectors.toList());

    Set<String> feedbackGroupIds = new HashSet<>();
    for (List<String> chunk : ListUtils.partition(inactiveGroupIds, GROUP_ID_LOOKUP_CHUNK_SIZE)) {
      sessionRepository.findByFeedbackGroupIdIn(new HashSet<>(chunk)).stream()
          .map(Session::getFeedbackGroupId)
          .filter(StringUtils::isNotBlank)
          .forEach(feedbackGroupIds::add);
    }
    return feedbackGroupIds;
  }

  private List<GroupDTO> fetchAllInactivePrivateGroups() {
    LocalDateTime dateTimeToCheck =
        CustomLocalDateTime.nowInUtc()
            .with(LocalTime.MIDNIGHT)
            .minusDays(sessionInactiveDeleteWorkflowCheckDays);
    try {
      return rocketChatService.fetchAllInactivePrivateGroupsSinceGivenDate(dateTimeToCheck);
    } catch (RocketChatGetGroupsListAllException ex) {
      LogService.logRocketChatError(ex);
    }
    return Collections.emptyList();
  }
}
