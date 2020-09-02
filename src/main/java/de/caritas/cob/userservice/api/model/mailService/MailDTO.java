package de.caritas.cob.userservice.api.model.mailService;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MailDTO {

  private String template;
  private String email;
  private List<TemplateDataDTO> templateData;

}