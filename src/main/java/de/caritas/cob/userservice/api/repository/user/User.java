package de.caritas.cob.userservice.api.repository.user;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Size;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import de.caritas.cob.userservice.api.repository.session.Session;
import de.caritas.cob.userservice.api.repository.useragency.UserAgency;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a user
 * 
 */
@Entity
@Table(name = "user")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class User {

  @Id
  @Column(name = "user_id", updatable = false, nullable = false)
  @Size(max = 36)
  @NonNull
  private String userId;

  @Column(name = "id_old", updatable = false)
  private Long oldId;

  @Column(name = "username", updatable = false, nullable = false)
  @Size(max = 255)
  @NonNull
  private String username;

  @Column(name = "email", nullable = false)
  @Size(max = 255)
  @NonNull
  private String email;

  @Column(name = "rc_user_id")
  private String rcUserId;

  @Column(name = "language_formal", nullable = false)
  @Type(type = "org.hibernate.type.NumericBooleanType")
  private boolean languageFormal;

  @OneToMany(mappedBy = "user")
  private Set<Session> sessions;

  @OneToMany(mappedBy = "user")
  private Set<UserAgency> userAgencies;

  @Column(name = "mobile_token")
  private String mobileToken;

  @Column(name = "delete_date")
  private LocalDateTime deleteDate;

  public User(@Size(max = 36) String userId, Long oldId, @Size(max = 255) String username,
      @Size(max = 255) String email, boolean languageFormal) {
    this.userId = userId;
    this.oldId = oldId;
    this.username = username;
    this.email = email;
    this.languageFormal = languageFormal;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof User)) {
      return false;
    }
    User user = (User) o;
    return userId.equals(user.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId);
  }
}
