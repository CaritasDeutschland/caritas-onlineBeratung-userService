package de.caritas.cob.userservice.api.service.consultant;

import static de.caritas.cob.userservice.api.helper.CustomLocalDateTime.nowInUtc;
import static java.util.Objects.nonNull;

import de.caritas.cob.userservice.api.exception.httpresponses.BadRequestException;
import de.caritas.cob.userservice.api.exception.httpresponses.ForbiddenException;
import de.caritas.cob.userservice.api.exception.httpresponses.NotFoundException;
import de.caritas.cob.userservice.api.helper.AuthenticatedUser;
import de.caritas.cob.userservice.api.model.Consultant;
import de.caritas.cob.userservice.api.service.ConsultantService;
import de.caritas.cob.userservice.api.service.agency.AgencyService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * CARITAS-976: Handles the optional registration redirect URLs of a consultant (personal, direct
 * link) and of the agencies the consultant is assigned to (shared agency link). The agency URL is
 * persisted in the agencyService; the membership check is performed here because the
 * consultant-agency relation only exists in the userService.
 */
@Service
@RequiredArgsConstructor
public class RegistrationUrlService {

  /** Only URLs containing this domain may be used as a registration redirect. */
  private static final String ALLOWED_REGISTRATION_DOMAIN = "caritas-onlineberatung.de";

  private final @NonNull AuthenticatedUser authenticatedUser;
  private final @NonNull ConsultantService consultantService;
  private final @NonNull AgencyService agencyService;

  @Value("${app.base.url}")
  private String appBaseUrl;

  /**
   * Sets/updates the personal registration redirect URL of the currently authenticated consultant.
   * A {@code null}/blank URL removes the override.
   */
  public void setConsultantRegistrationUrl(String registrationUrl) {
    var consultant = getAuthenticatedConsultant();
    if (isBlank(registrationUrl)) {
      clearConsultantRegistrationUrl(consultant);
      return;
    }
    validateRegistrationUrl(registrationUrl);
    consultant.setRegistrationUrl(registrationUrl.trim());
    consultant.setRegistrationUrlAddedDate(nowInUtc());
    consultant.setUpdateDate(nowInUtc());
    consultantService.saveConsultant(consultant);
  }

  /** Removes the personal registration redirect URL of the currently authenticated consultant. */
  public void deleteConsultantRegistrationUrl() {
    clearConsultantRegistrationUrl(getAuthenticatedConsultant());
  }

  /**
   * Resolves the target of the public registration redirect for a consultant: the override URL if
   * set, otherwise the default personal registration deep link.
   */
  public String resolveConsultantRedirectTarget(String consultantId) {
    var consultant =
        consultantService
            .getConsultant(consultantId)
            .orElseThrow(
                () -> new NotFoundException("Consultant with id %s not found", consultantId));
    if (nonNull(consultant.getRegistrationUrl()) && !consultant.getRegistrationUrl().isBlank()) {
      return consultant.getRegistrationUrl();
    }
    return appBaseUrl + "/registration?cid=" + consultantId;
  }

  /**
   * Sets/updates the shared registration redirect URL of an agency the authenticated consultant is
   * assigned to. A {@code null}/blank URL removes the override.
   */
  public void setAgencyRegistrationUrl(Long agencyId, String registrationUrl) {
    var consultant = getAuthenticatedConsultant();
    verifyAgencyMembership(consultant, agencyId);
    if (!isBlank(registrationUrl)) {
      validateRegistrationUrl(registrationUrl);
    }
    agencyService.setAgencyRegistrationUrl(
        agencyId, isBlank(registrationUrl) ? null : registrationUrl.trim(), consultant.getId());
  }

  /**
   * Removes the shared registration redirect URL of an agency the authenticated consultant is
   * assigned to.
   */
  public void deleteAgencyRegistrationUrl(Long agencyId) {
    var consultant = getAuthenticatedConsultant();
    verifyAgencyMembership(consultant, agencyId);
    agencyService.deleteAgencyRegistrationUrl(agencyId);
  }

  private void clearConsultantRegistrationUrl(Consultant consultant) {
    consultant.setRegistrationUrl(null);
    consultant.setRegistrationUrlAddedDate(null);
    consultant.setUpdateDate(nowInUtc());
    consultantService.saveConsultant(consultant);
  }

  private Consultant getAuthenticatedConsultant() {
    var consultantId = authenticatedUser.getUserId();
    return consultantService
        .getConsultant(consultantId)
        .orElseThrow(() -> new NotFoundException("Consultant with id %s not found", consultantId));
  }

  private void verifyAgencyMembership(Consultant consultant, Long agencyId) {
    if (!consultant.isInAgency(agencyId)) {
      throw new ForbiddenException(
          String.format(
              "Consultant with id %s is not assigned to agency %s", consultant.getId(), agencyId));
    }
  }

  private void validateRegistrationUrl(String registrationUrl) {
    if (!registrationUrl.toLowerCase().contains(ALLOWED_REGISTRATION_DOMAIN)) {
      throw new BadRequestException(
          String.format(
              "Registration url must contain the domain %s", ALLOWED_REGISTRATION_DOMAIN));
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
