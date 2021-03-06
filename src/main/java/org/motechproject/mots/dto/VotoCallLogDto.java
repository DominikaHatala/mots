package org.motechproject.mots.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VotoCallLogDto {

  @Getter
  @Setter
  private String logId;

  @Getter
  @Setter
  private String chwIvrId;

  @Getter
  @Setter
  private List<VotoBlockDto> interactions;

  @JsonProperty("delivery_log")
  private void unpackNesteObject(Map<String, String> log) {
    logId = log.get("id");
    chwIvrId = log.get("subscriber_id");
  }
}
