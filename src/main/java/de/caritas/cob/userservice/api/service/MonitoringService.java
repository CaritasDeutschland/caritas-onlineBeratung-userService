package de.caritas.cob.userservice.api.service;

import de.caritas.cob.userservice.api.exception.httpresponses.InternalServerErrorException;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import de.caritas.cob.userservice.api.container.CreateEnquiryExceptionInformation;
import de.caritas.cob.userservice.api.exception.CreateMonitoringException;
import de.caritas.cob.userservice.api.helper.MonitoringHelper;
import de.caritas.cob.userservice.api.manager.consultingType.ConsultingTypeSettings;
import de.caritas.cob.userservice.api.model.MonitoringDTO;
import de.caritas.cob.userservice.api.repository.monitoring.Monitoring;
import de.caritas.cob.userservice.api.repository.monitoring.MonitoringRepository;
import de.caritas.cob.userservice.api.repository.monitoring.MonitoringType;
import de.caritas.cob.userservice.api.repository.monitoringOption.MonitoringOption;
import de.caritas.cob.userservice.api.repository.session.ConsultingType;
import de.caritas.cob.userservice.api.repository.session.Session;

/**
 * Service for {@link Monitoring}
 */
@Service
public class MonitoringService {

  private MonitoringRepository monitoringRepository;
  private MonitoringHelper monitoringHelper;

  @Autowired
  public MonitoringService(MonitoringRepository monitoringRepository,
      MonitoringHelper monitoringHelper) {
    this.monitoringRepository = monitoringRepository;
    this.monitoringHelper = monitoringHelper;
  }

  /**
   * Creates and inserts the initial monitoring data for the given {@link Session} into the database
   * if monitoring is activated for the given {@link ConsultingTypeSettings}
   * 
   * @param session {@link Session}
   * @param consultingTypeSettings {@link ConsultingTypeSettings}
   * @throws CreateMonitoringException
   */
  public void createMonitoring(Session session, ConsultingTypeSettings consultingTypeSettings)
      throws CreateMonitoringException {

    if (session != null && consultingTypeSettings.isMonitoring()) {
      try {
        updateMonitoring(session.getId(),
            monitoringHelper.getMonitoringInitalList(session.getConsultingType()));
      } catch (Exception exception) {
        CreateEnquiryExceptionInformation exceptionInformation = CreateEnquiryExceptionInformation
            .builder().session(session).rcGroupId(session.getGroupId()).build();
        throw new CreateMonitoringException(
            String.format("Could not create monitoring for session %s with consultingType %s",
                session.getId(), consultingTypeSettings.getConsultingType()),
            exception, exceptionInformation);
      }
    }
  }

  /**
   * Returns the monitoring for the given session
   * 
   * @param session
   * @return
   */
  public MonitoringDTO getMonitoring(Session session) {

    try {
      List<Monitoring> monitoring = monitoringRepository.findBySessionId(session.getId());
      return new MonitoringDTO(convertToMonitoringMap(monitoring, session.getConsultingType()));

    } catch (DataAccessException ex) {
      throw new InternalServerErrorException("Database error while saving monitoring data.",
          LogService::logDatabaseError);
    }
  }

  /**
   * Updates the monitoring values of a {@link Session}
   * 
   * @param sessionId
   * @param monitoringDTO
   */
  public void updateMonitoring(Long sessionId, MonitoringDTO monitoringDTO) {

    try {
      List<Monitoring> monitoringList =
          monitoringHelper.createMonitoringList(monitoringDTO, sessionId);

      monitoringRepository.saveAll(monitoringList);

    } catch (DataAccessException ex) {
      throw new InternalServerErrorException("Database error while saving monitoring data.",
          LogService::logDatabaseError);
    }
  }

  /**
   * Deletes the monitoring values of a {@link Session}
   * 
   * @param sessionId
   * @param monitoringDTO
   * @return
   */
  public void deleteMonitoring(Long sessionId, MonitoringDTO monitoringDTO) {

    try {
      List<Monitoring> monitoringList =
          monitoringHelper.createMonitoringList(monitoringDTO, sessionId);

      monitoringRepository.deleteAll(monitoringList);

    } catch (DataAccessException ex) {
      throw new InternalServerErrorException("Database error while deleting monitoring data.",
          LogService::logDatabaseError);
    }
  }

  /**
   * Converts a list of {@link Monitoring} and returns a {@link LinkedHashMap} of
   * {@link MonitoringDTO} on level 0 (addictiveDrugs, intervention, etc.)
   * 
   * @param monitoringList
   * @return
   */
  private LinkedHashMap<String, Object> convertToMonitoringMap(List<Monitoring> monitoringList,
      ConsultingType consultingType) {

    LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();

    if (monitoringList != null) {
      for (MonitoringType type : MonitoringType.values()) {
        if (type.getConsultingType().equals(consultingType)) {
          map.put(type.getKey() != null ? type.getKey() : null,
              convertToMonitoring(type, monitoringList));
        }
      }
    }

    return monitoringHelper.sortMonitoringMap(map, consultingType);
  }

  /**
   * Converts a list of {@link Monitoring} and returns a {@link LinkedHashMap} of
   * {@link MonitoringDTO} on level 1 (monitoring table - {@link Monitoring})
   * 
   * @param type
   * @param monitoringList
   * @return
   */
  private LinkedHashMap<String, Object> convertToMonitoring(MonitoringType type,
      List<Monitoring> monitoringList) {

    LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();

    for (Monitoring monitoring : monitoringList) {
      if (monitoring.getMonitoringType().getKey().equals(type.getKey())) {
        map.put(monitoring.getKey(), monitoring.getValue() != null ? (Boolean) monitoring.getValue()
            : convertToMonitoringOption(type, monitoring.getKey(), monitoringList));
      }
    }

    return map;
  }

  /**
   * Converts a list of {@link Monitoring} and returns a {@link LinkedHashMap} of
   * {@link MonitoringDTO} on level 2 (monitoring_option table - {@link MonitoringOption}).
   * 
   * @param type
   * @param monitoringKey
   * @param monitoringList
   * @return
   */
  private LinkedHashMap<String, Object> convertToMonitoringOption(MonitoringType type,
      String monitoringKey, List<Monitoring> monitoringList) {
    LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();

    for (Monitoring monitoring : monitoringList) {
      if (monitoring.getMonitoringType().getKey().equals(type.getKey())
          && monitoringKey.equals(monitoring.getKey())) {
        for (MonitoringOption option : monitoring.getMonitoringOptionList()) {
          map.put(option.getKey(), (Boolean) option.getValue());
        }
      }
    }

    return map;
  }

  /**
   * Roll back the initialization of the monitoring data for a {@link Session}.
   * 
   * @param session {@link Session}
   */
  public void rollbackInitializeMonitoring(Session session) {
    if (session != null) {
      try {
        deleteMonitoring(session.getId(),
            monitoringHelper.getMonitoringInitalList(session.getConsultingType()));

      } catch (InternalServerErrorException ex) {
        LogService.logInternalServerError(String.format(
            "Error during monitoring rollback. Monitoring data could not be deleted for session: %s",
            session.toString()), ex);
      }
    }
  }
}