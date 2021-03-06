package org.motechproject.mots.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;
import org.motechproject.mots.constants.ValidationMessages;
import org.motechproject.mots.validate.annotations.Uuid;

public class LocationPreviewDto {

  @Getter
  @Setter
  @Uuid
  private String id;

  @Getter
  @Setter
  @NotBlank(message = ValidationMessages.EMPTY_LOCATION_NAME)
  private String name;

  @Setter
  @Getter
  @JsonInclude(Include.NON_NULL)
  private String parent;

  @Setter
  @Getter
  @JsonInclude(Include.NON_NULL)
  private String districtName;

  @Setter
  @Getter
  @JsonInclude(Include.NON_NULL)
  private String chiefdomName;

  @Setter
  @Getter
  @JsonInclude(Include.NON_NULL)
  private String facilityType;

  @Setter
  @Getter
  @JsonInclude(Include.NON_NULL)
  private String facilityId;

  @Setter
  @Getter
  @JsonInclude(Include.NON_NULL)
  private String inchargeFullName;

  @Getter
  @Setter
  private String ownerUsername;
}
