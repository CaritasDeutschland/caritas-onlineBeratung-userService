package de.caritas.cob.userservice.api.admin.service.agency;

import static de.caritas.cob.userservice.api.exception.httpresponses.customheader.HttpStatusExceptionReason.CONSULTANT_IS_THE_LAST_OF_AGENCY_AND_AGENCY_HAS_OPEN_ENQUIRIES;
import static de.caritas.cob.userservice.api.exception.httpresponses.customheader.HttpStatusExceptionReason.CONSULTANT_IS_THE_LAST_OF_AGENCY_AND_AGENCY_IS_STILL_ACTIVE;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.caritas.cob.userservice.api.exception.httpresponses.CustomValidationHttpStatusException;
import de.caritas.cob.userservice.api.exception.httpresponses.InternalServerErrorException;
import de.caritas.cob.userservice.api.model.AgencyDTO;
import de.caritas.cob.userservice.api.repository.consultantagency.ConsultantAgency;
import de.caritas.cob.userservice.api.repository.consultantagency.ConsultantAgencyRepository;
import de.caritas.cob.userservice.api.repository.session.Session;
import de.caritas.cob.userservice.api.repository.session.SessionRepository;
import de.caritas.cob.userservice.api.service.agency.AgencyService;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConsultantAgencyDeletionValidationServiceTest {

  @InjectMocks
  private ConsultantAgencyDeletionValidationService agencyDeletionValidationService;

  @Mock
  private ConsultantAgencyRepository consultantAgencyRepository;

  @Mock
  private AgencyService agencyService;

  @Mock
  private SessionRepository sessionRepository;

  @Test
  public void validateForDeletion_Should_throwCustomValidationHttpStatusException_When_consultantIsTheLastOfTheAgencyAndAgencyIsStillOnline() {
    ConsultantAgency consultantAgency = new EasyRandom().nextObject(ConsultantAgency.class);
    consultantAgency.setDeleteDate(null);
    when(this.consultantAgencyRepository.findByAgencyIdAndDeleteDateIsNull(any()))
        .thenReturn(singletonList(consultantAgency));
    when(this.agencyService.getAgency(any())).thenReturn(new AgencyDTO().offline(false));

    try {
      this.agencyDeletionValidationService.validateForDeletion(consultantAgency);
      fail("Exception was not thrown");
    } catch (CustomValidationHttpStatusException e) {
      assertThat(requireNonNull(e.getCustomHttpHeader().get("X-Reason")).iterator().next(),
          is(CONSULTANT_IS_THE_LAST_OF_AGENCY_AND_AGENCY_IS_STILL_ACTIVE.name()));
    }
  }

  @Test
  public void validateForDeletion_Should_throwCustomValidationHttpStatusException_When_consultantIsTheLastOfTheAgencyAndAgencyHasOpenEnquiries() {
    ConsultantAgency consultantAgency = new EasyRandom().nextObject(ConsultantAgency.class);
    consultantAgency.setDeleteDate(null);
    when(this.consultantAgencyRepository.findByAgencyIdAndDeleteDateIsNull(any()))
        .thenReturn(singletonList(consultantAgency));
    when(this.agencyService.getAgency(any())).thenReturn(new AgencyDTO().offline(true));
    when(this.sessionRepository.findByAgencyIdAndStatusAndConsultantIsNull(any(), any()))
        .thenReturn(singletonList(mock(Session.class)));

    try {
      this.agencyDeletionValidationService.validateForDeletion(consultantAgency);
      fail("Exception was not thrown");
    } catch (CustomValidationHttpStatusException e) {
      assertThat(requireNonNull(e.getCustomHttpHeader().get("X-Reason")).iterator().next(),
          is(CONSULTANT_IS_THE_LAST_OF_AGENCY_AND_AGENCY_HAS_OPEN_ENQUIRIES.name()));
    }
  }

  @Test(expected = InternalServerErrorException.class)
  public void validateForDeletion_Should_throwInternalServerErrorException_When_agencyCanNotBeFetched() {
    ConsultantAgency consultantAgency = new EasyRandom().nextObject(ConsultantAgency.class);
    consultantAgency.setDeleteDate(null);
    when(this.consultantAgencyRepository.findByAgencyIdAndDeleteDateIsNull(any()))
        .thenReturn(singletonList(consultantAgency));
    when(this.agencyService.getAgency(any())).thenThrow(new InternalServerErrorException(""));

    this.agencyDeletionValidationService.validateForDeletion(consultantAgency);
  }

  @Test
  public void validateForDeletion_Should_notThrowAnyException_When_consultantAgencyIsValidForDeletion() {
    ConsultantAgency consultantAgency = new EasyRandom().nextObject(ConsultantAgency.class);
    consultantAgency.setDeleteDate(null);
    when(this.consultantAgencyRepository.findByAgencyIdAndDeleteDateIsNull(any()))
        .thenReturn(singletonList(consultantAgency));
    when(this.agencyService.getAgency(any())).thenReturn(new AgencyDTO().offline(true));

    assertDoesNotThrow(
        () -> this.agencyDeletionValidationService.validateForDeletion(consultantAgency));
  }

}
