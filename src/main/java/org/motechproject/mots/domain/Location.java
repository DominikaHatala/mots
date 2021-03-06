package org.motechproject.mots.domain;

import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.NotBlank;
import org.motechproject.mots.constants.ValidationMessages;
import org.motechproject.mots.domain.audit.AuditListener;
import org.motechproject.mots.domain.audit.Auditable;
import org.motechproject.mots.domain.enums.FacilityType;
import org.motechproject.mots.domain.security.User;

@MappedSuperclass
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
@EntityListeners(AuditListener.class)
public abstract class Location extends BaseTimestampedEntity implements Auditable {

  @Column(name = "name", nullable = false)
  @Getter
  @Setter
  @NonNull
  @NotBlank(message = ValidationMessages.EMPTY_LOCATION_NAME)
  private String name;

  @OneToOne
  @JoinColumn(name = "owner_id", nullable = false)
  @Getter
  @Setter
  private User owner;

  public Location(UUID id) {
    super(id);
  }

  public abstract String getParentName();

  public String getChiefdomName() {
    return null;
  }

  public String getDistrictName() {
    return null;
  }

  public FacilityType getFacilityType() {
    return null;
  }

  public String getFacilityId() {
    return null;
  }

  public String getInchargeFullName() {
    return null;
  }
}
