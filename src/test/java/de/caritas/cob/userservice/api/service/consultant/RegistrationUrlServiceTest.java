package de.caritas.cob.userservice.api.service.consultant;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.caritas.cob.userservice.api.exception.httpresponses.BadRequestException;
import de.caritas.cob.userservice.api.exception.httpresponses.ForbiddenException;
import de.caritas.cob.userservice.api.exception.httpresponses.NotFoundException;
import de.caritas.cob.userservice.api.helper.AuthenticatedUser;
import de.caritas.cob.userservice.api.model.Consultant;
import de.caritas.cob.userservice.api.service.ConsultantService;
import de.caritas.cob.userservice.api.service.agency.AgencyService;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

/** CARITAS-976 unit tests for {@link RegistrationUrlService}. */
@RunWith(MockitoJUnitRunner.class)
public class RegistrationUrlServiceTest {

  private static final String CONSULTANT_ID = "aadc0ecf-c048-4bfc-857d-8c9b2e425500";
  private static final Long AGENCY_ID = 98L;
  private static final String VALID_URL = "https://caritas-onlineberatung.de/registration/max";
  private static final String INVALID_URL = "https://evil.example.com/registration";

  @InjectMocks private RegistrationUrlService underTest;

  @Mock private AuthenticatedUser authenticatedUser;
  @Mock private ConsultantService consultantService;
  @Mock private AgencyService agencyService;

  @Before
  public void setup() {
    ReflectionTestUtils.setField(underTest, "appBaseUrl", "https://app.example.com");
  }

  private Consultant mockAuthenticatedConsultant() {
    var consultant = Mockito.mock(Consultant.class);
    // getId() is only read on some paths (e.g. addedBy); keep lenient to avoid strict-stub failures
    Mockito.lenient().when(consultant.getId()).thenReturn(CONSULTANT_ID);
    when(authenticatedUser.getUserId()).thenReturn(CONSULTANT_ID);
    when(consultantService.getConsultant(CONSULTANT_ID)).thenReturn(Optional.of(consultant));
    return consultant;
  }

  // --- consultant personal url ---

  @Test
  public void setConsultantRegistrationUrl_Should_persistUrlAndAttribution_When_valid() {
    var consultant = mockAuthenticatedConsultant();

    underTest.setConsultantRegistrationUrl(VALID_URL);

    verify(consultant).setRegistrationUrl(VALID_URL);
    verify(consultant).setRegistrationUrlAddedDate(any());
    verify(consultantService).saveConsultant(consultant);
  }

  @Test(expected = BadRequestException.class)
  public void setConsultantRegistrationUrl_Should_throwBadRequest_When_domainNotAllowed() {
    mockAuthenticatedConsultant();

    underTest.setConsultantRegistrationUrl(INVALID_URL);
  }

  @Test
  public void setConsultantRegistrationUrl_Should_clearOverride_When_blank() {
    var consultant = mockAuthenticatedConsultant();

    underTest.setConsultantRegistrationUrl("   ");

    verify(consultant).setRegistrationUrl(null);
    verify(consultant).setRegistrationUrlAddedDate(null);
    verify(consultantService).saveConsultant(consultant);
  }

  @Test
  public void deleteConsultantRegistrationUrl_Should_clearOverride() {
    var consultant = mockAuthenticatedConsultant();

    underTest.deleteConsultantRegistrationUrl();

    verify(consultant).setRegistrationUrl(null);
    verify(consultant).setRegistrationUrlAddedDate(null);
    verify(consultantService).saveConsultant(consultant);
  }

  @Test
  public void resolveConsultantRedirectTarget_Should_returnOverride_When_present() {
    var consultant = Mockito.mock(Consultant.class);
    when(consultant.getRegistrationUrl()).thenReturn(VALID_URL);
    when(consultantService.getConsultant(CONSULTANT_ID)).thenReturn(Optional.of(consultant));

    assertEquals(VALID_URL, underTest.resolveConsultantRedirectTarget(CONSULTANT_ID));
  }

  @Test
  public void resolveConsultantRedirectTarget_Should_returnDefaultDeepLink_When_noOverride() {
    var consultant = Mockito.mock(Consultant.class);
    when(consultant.getRegistrationUrl()).thenReturn(null);
    when(consultantService.getConsultant(CONSULTANT_ID)).thenReturn(Optional.of(consultant));

    assertEquals(
        "https://app.example.com/registration?cid=" + CONSULTANT_ID,
        underTest.resolveConsultantRedirectTarget(CONSULTANT_ID));
  }

  @Test(expected = NotFoundException.class)
  public void resolveConsultantRedirectTarget_Should_throwNotFound_When_consultantMissing() {
    when(consultantService.getConsultant(CONSULTANT_ID)).thenReturn(Optional.empty());

    underTest.resolveConsultantRedirectTarget(CONSULTANT_ID);
  }

  // --- agency url (membership gated) ---

  @Test
  public void setAgencyRegistrationUrl_Should_delegateToAgencyService_When_memberAndValid() {
    var consultant = mockAuthenticatedConsultant();
    when(consultant.isInAgency(AGENCY_ID)).thenReturn(true);

    underTest.setAgencyRegistrationUrl(AGENCY_ID, VALID_URL);

    verify(agencyService).setAgencyRegistrationUrl(AGENCY_ID, VALID_URL, CONSULTANT_ID);
  }

  @Test
  public void setAgencyRegistrationUrl_Should_clearOverride_When_blank() {
    var consultant = mockAuthenticatedConsultant();
    when(consultant.isInAgency(AGENCY_ID)).thenReturn(true);

    underTest.setAgencyRegistrationUrl(AGENCY_ID, "  ");

    verify(agencyService).setAgencyRegistrationUrl(eq(AGENCY_ID), isNull(), eq(CONSULTANT_ID));
  }

  @Test(expected = ForbiddenException.class)
  public void setAgencyRegistrationUrl_Should_throwForbidden_When_notMember() {
    var consultant = mockAuthenticatedConsultant();
    when(consultant.isInAgency(AGENCY_ID)).thenReturn(false);

    try {
      underTest.setAgencyRegistrationUrl(AGENCY_ID, VALID_URL);
    } finally {
      verify(agencyService, never()).setAgencyRegistrationUrl(any(), any(), any());
    }
  }

  @Test(expected = BadRequestException.class)
  public void setAgencyRegistrationUrl_Should_throwBadRequest_When_domainNotAllowed() {
    var consultant = mockAuthenticatedConsultant();
    when(consultant.isInAgency(AGENCY_ID)).thenReturn(true);

    try {
      underTest.setAgencyRegistrationUrl(AGENCY_ID, INVALID_URL);
    } finally {
      verify(agencyService, never()).setAgencyRegistrationUrl(any(), any(), any());
    }
  }

  @Test
  public void deleteAgencyRegistrationUrl_Should_delegateToAgencyService_When_member() {
    var consultant = mockAuthenticatedConsultant();
    when(consultant.isInAgency(AGENCY_ID)).thenReturn(true);

    underTest.deleteAgencyRegistrationUrl(AGENCY_ID);

    verify(agencyService).deleteAgencyRegistrationUrl(AGENCY_ID);
  }

  @Test(expected = ForbiddenException.class)
  public void deleteAgencyRegistrationUrl_Should_throwForbidden_When_notMember() {
    var consultant = mockAuthenticatedConsultant();
    when(consultant.isInAgency(AGENCY_ID)).thenReturn(false);

    try {
      underTest.deleteAgencyRegistrationUrl(AGENCY_ID);
    } finally {
      verify(agencyService, never()).deleteAgencyRegistrationUrl(any());
    }
  }
}
