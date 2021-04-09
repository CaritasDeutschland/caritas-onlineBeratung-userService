package de.caritas.cob.userservice.api.admin.service.agency;

import static de.caritas.cob.userservice.config.CachingConfig.AGENCY_CACHE;

import de.caritas.cob.userservice.agencyadminserivce.generated.ApiClient;
import de.caritas.cob.userservice.agencyadminserivce.generated.web.AdminAgencyControllerApi;
import de.caritas.cob.userservice.agencyadminserivce.generated.web.model.AgencyAdminResponseDTO;
import de.caritas.cob.userservice.api.service.securityheader.SecurityHeaderSupplier;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

/**
 * Service to wrap admin api call for agencies.
 */
@Service
@RequiredArgsConstructor
public class AgencyAdminService {

  private final @NonNull AdminAgencyControllerApi adminAgencyControllerApi;
  private final @NonNull SecurityHeaderSupplier securityHeaderSupplier;

  /**
   * Retrieves all agencies provided by agency service. Important hint: Depending on the amount
   * of existing agencies that call might need a few seconds to be performed.
   *
   * @return all existing agencies
   */
  @Cacheable(AGENCY_CACHE)
  public List<AgencyAdminResponseDTO> retrieveAllAgencies() {
    addDefaultHeaders(this.adminAgencyControllerApi.getApiClient());
    return this.adminAgencyControllerApi.searchAgencies(0, Integer.MAX_VALUE, null)
        .getEmbedded();
  }

  private void addDefaultHeaders(ApiClient apiClient) {
    HttpHeaders headers = this.securityHeaderSupplier.getKeycloakAndCsrfHttpHeaders();
    headers.forEach((key, value) -> apiClient.addDefaultHeader(key, value.iterator().next()));
  }

}