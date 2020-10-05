package de.caritas.cob.userservice.api.service.emailsupplier;

import static de.caritas.cob.userservice.api.helper.EmailNotificationHelper.TEMPLATE_NEW_ENQUIRY_NOTIFICATION;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import de.caritas.cob.userservice.api.exception.AgencyServiceHelperException;
import de.caritas.cob.userservice.api.model.AgencyDTO;
import de.caritas.cob.userservice.api.model.mailservice.MailDTO;
import de.caritas.cob.userservice.api.model.mailservice.TemplateDataDTO;
import de.caritas.cob.userservice.api.repository.consultantAgency.ConsultantAgency;
import de.caritas.cob.userservice.api.repository.consultantAgency.ConsultantAgencyRepository;
import de.caritas.cob.userservice.api.repository.session.Session;
import de.caritas.cob.userservice.api.service.helper.AgencyServiceHelper;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

/**
 * Supplier to provide mails to be sent when a new enquiry was created.
 */
@AllArgsConstructor
public class NewEnquiryEmailSupplier implements EmailSupplier {

  private final Session session;
  private final ConsultantAgencyRepository consultantAgencyRepository;
  private final AgencyServiceHelper agencyServiceHelper;
  private final String applicationBaseUrl;

  /**
   * Generates the enquiry notification mails sent to regarding consultants when a new enquiry
   * has been created.
   *
   * @return a list of the generated {@link MailDTO}
   */
  @Override
  public List<MailDTO> generateEmails() throws AgencyServiceHelperException {
    List<ConsultantAgency> consultantAgencyList =
        consultantAgencyRepository.findByAgencyId(session.getAgencyId());

    if (isEmpty(consultantAgencyList)) {
      return emptyList();
    }
    AgencyDTO agency = agencyServiceHelper.getAgency(session.getAgencyId());
    return consultantAgencyList.stream()
        .filter(this::validConsultantAgency)
        .map(toEnquiryMailDTO(agency))
        .collect(Collectors.toList());
  }

  private Boolean validConsultantAgency(ConsultantAgency consultantAgency) {
    return nonNull(consultantAgency)
        && isNotBlank(consultantAgency.getConsultant().getEmail())
        && !consultantAgency.getConsultant().isAbsent();
  }

  private Function<ConsultantAgency, MailDTO> toEnquiryMailDTO(AgencyDTO agency) {
    return consultantAgency -> buildMailDtoForNewEnquiryNotificationConsultant(
        consultantAgency.getConsultant().getEmail(),
        consultantAgency.getConsultant().getFullName(),
        session.getPostcode(),
        agency.getName()
    );
  }

  private MailDTO buildMailDtoForNewEnquiryNotificationConsultant(String email, String name,
      String postCode, String agency) {
    return MailDTO.builder()
        .template(TEMPLATE_NEW_ENQUIRY_NOTIFICATION)
        .email(email)
        .templateData(asList(
            new TemplateDataDTO("name", name),
            new TemplateDataDTO("plz", postCode),
            new TemplateDataDTO("beratungsstelle", agency),
            new TemplateDataDTO("url", applicationBaseUrl)))
        .build();
  }

}