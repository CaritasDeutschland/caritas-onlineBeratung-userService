package de.caritas.cob.userservice.api.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Push service to send new message notifications via firebase to mobile devices.
 */
@Service
public class PushMessageService {

  @Value("${firebase.configuration.push-notifications.enabled}")
  private boolean isEnabled;

  @Value("${firebase.configuration.credentials.file.path}")
  private String firebaseConfiguration;

  @Value("${firebase.configuration.notification.message}")
  private String pushNotificationMessage;

  private FirebaseMessaging firebaseMessaging;

  /**
   * Initializes the basic firebase configuration.
   */
  @SneakyThrows
  @EventListener(ApplicationReadyEvent.class)
  public void initializeFirebase() {
    if (this.isEnabled) {
      Path path = FileUtils.getFile(firebaseConfiguration).toPath();
      InputStream inputStream = Files.newInputStream(path);

      FirebaseOptions options = FirebaseOptions.builder()
          .setCredentials(GoogleCredentials.fromStream(inputStream))
          .build();

      FirebaseApp.initializeApp(options);
      this.firebaseMessaging = FirebaseMessaging.getInstance();
    } else {
      LogService.logInfo("Firebase push notifications are disabled");
    }
  }

  /**
   * Sends a push notification message to mobile device with given registration token.
   *
   * @param registrationToken the mobile device identifier
   */
  public void pushNewMessageEvent(String registrationToken) {
    if (!this.isEnabled) {
      return;
    }
    Message message = Message.builder()
        .setNotification(Notification.builder()
            .setBody(pushNotificationMessage)
            .build())
        .setToken(registrationToken)
        .build();

    try {
      this.firebaseMessaging.send(message);
    } catch (FirebaseMessagingException e) {
      LogService.logWarn(e);
    }
  }

}
