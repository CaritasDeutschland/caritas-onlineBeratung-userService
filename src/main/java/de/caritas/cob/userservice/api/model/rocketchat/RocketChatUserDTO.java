package de.caritas.cob.userservice.api.model.rocketchat;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.caritas.cob.userservice.api.model.rocketchat.user.UserRoomDTO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RocketChatUserDTO {

  @JsonProperty("_id")
  private String id;
  private String username;
  private List<UserRoomDTO> rooms;

}
