package de.caritas.cob.userservice.api.service.user.anonymous;

import static java.lang.Integer.parseInt;
import static java.util.Collections.sort;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import de.caritas.cob.userservice.api.helper.UserHelper;
import de.caritas.cob.userservice.api.service.user.UserService;
import java.util.LinkedList;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Registry to generate, hold and handle all current anonymous usernames.
 */
@Component
@RequiredArgsConstructor
public class AnonymousUsernameRegistry {

  private final @NonNull UserService userService;
  private final @NonNull UserHelper userHelper;

  @Value("${anonymous.username.prefix}")
  private String usernamePrefix;

  private static final LinkedList<Integer> idRegistry = new LinkedList<>();

  /**
   * Generates an unique anonymous username.
   *
   * @return encoded unique anonymous username
   */
  public synchronized String generateUniqueUsername() {

    String username;
    do {
      username = generateUsername();
      idRegistry.add(obtainUsernameId(username));
    } while (isUsernameOccupied(username));


    return userHelper.encodeUsername(username);
  }

  private String generateUsername() {
    return usernamePrefix + obtainSmallestPossibleId();
  }

  private int obtainSmallestPossibleId() {

    var smallestId = 1;
    sort(idRegistry);

    for (int i : idRegistry) {
      if (smallestId < i) {
        return smallestId;
      }
      smallestId = i + 1;
    }

    return smallestId;
  }

  private boolean isUsernameOccupied(String username) {
    return userService.findUserByUsername(username)
        .stream()
        .count() > 0;
  }

  private int obtainUsernameId(String username) {
    return parseInt(substringAfter(username, usernamePrefix));
  }
}