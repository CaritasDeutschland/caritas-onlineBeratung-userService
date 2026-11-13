package de.caritas.cob.userservice.api.adapters.rocketchat;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GroupInfoDTO {

  @JsonProperty("success")
  private boolean success;

  @JsonProperty("error")
  private String error;

  @JsonProperty("errorType")
  private String errorType;

  @JsonProperty("group")
  private Object group;

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getErrorType() {
    return errorType;
  }

  public void setErrorType(String errorType) {
    this.errorType = errorType;
  }

  public Object getGroup() {
    return group;
  }

  public void setGroup(Object group) {
    this.group = group;
  }
}
