package com.scos.modules.facility.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "scos_facilities")
public class FacilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @NotBlank
    @Column(name = "facility_code", nullable = false, length = 40)
    private String facilityCode;

    @NotBlank
    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private FacilityCategory category;

    @Column(name = "building_id")
    private UUID buildingId;

    @Column(name = "location_details", length = 200)
    private String locationDetails;

    @Min(1)
    @Column(name = "capacity", nullable = false)
    private Integer capacity = 1;

    @DecimalMin("0.00")
    @Column(name = "hourly_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyFee = BigDecimal.ZERO;

    @DecimalMin("0.00")
    @Column(name = "security_deposit", nullable = false, precision = 10, scale = 2)
    private BigDecimal securityDeposit = BigDecimal.ZERO;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "INR";

    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval = false;

    @NotNull
    @Column(name = "operating_hours_start", nullable = false)
    private LocalTime operatingHoursStart = LocalTime.of(6, 0);

    @NotNull
    @Column(name = "operating_hours_end", nullable = false)
    private LocalTime operatingHoursEnd = LocalTime.of(22, 0);

    @Min(1)
    @Column(name = "min_booking_hours", nullable = false)
    private Integer minBookingHours = 1;

    @Min(1)
    @Column(name = "max_booking_hours", nullable = false)
    private Integer maxBookingHours = 6;

    @Min(0)
    @Column(name = "cancellation_cutoff_hours", nullable = false)
    private Integer cancellationCutoffHours = 2;

    @Column(name = "rules_and_regulations", columnDefinition = "TEXT")
    private String rulesAndRegulations;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by", nullable = false, length = 60)
    private String createdBy = "system";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_by", nullable = false, length = 60)
    private String updatedBy = "system";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public FacilityEntity() {
    }

    public FacilityEntity(UUID communityId, String facilityCode, String name, FacilityCategory category) {
        this.communityId = communityId;
        this.facilityCode = facilityCode;
        this.name = name;
        this.category = category;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (hourlyFee == null) {
            hourlyFee = BigDecimal.ZERO;
        }
        if (securityDeposit == null) {
            securityDeposit = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public void setCommunityId(UUID communityId) {
        this.communityId = communityId;
    }

    public String getFacilityCode() {
        return facilityCode;
    }

    public void setFacilityCode(String facilityCode) {
        this.facilityCode = facilityCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FacilityCategory getCategory() {
        return category;
    }

    public void setCategory(FacilityCategory category) {
        this.category = category;
    }

    public UUID getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(UUID buildingId) {
        this.buildingId = buildingId;
    }

    public String getLocationDetails() {
        return locationDetails;
    }

    public void setLocationDetails(String locationDetails) {
        this.locationDetails = locationDetails;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getHourlyFee() {
        return hourlyFee;
    }

    public void setHourlyFee(BigDecimal hourlyFee) {
        this.hourlyFee = hourlyFee;
    }

    public BigDecimal getSecurityDeposit() {
        return securityDeposit;
    }

    public void setSecurityDeposit(BigDecimal securityDeposit) {
        this.securityDeposit = securityDeposit;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public Boolean getRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(Boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public LocalTime getOperatingHoursStart() {
        return operatingHoursStart;
    }

    public void setOperatingHoursStart(LocalTime operatingHoursStart) {
        this.operatingHoursStart = operatingHoursStart;
    }

    public LocalTime getOperatingHoursEnd() {
        return operatingHoursEnd;
    }

    public void setOperatingHoursEnd(LocalTime operatingHoursEnd) {
        this.operatingHoursEnd = operatingHoursEnd;
    }

    public Integer getMinBookingHours() {
        return minBookingHours;
    }

    public void setMinBookingHours(Integer minBookingHours) {
        this.minBookingHours = minBookingHours;
    }

    public Integer getMaxBookingHours() {
        return maxBookingHours;
    }

    public void setMaxBookingHours(Integer maxBookingHours) {
        this.maxBookingHours = maxBookingHours;
    }

    public Integer getCancellationCutoffHours() {
        return cancellationCutoffHours;
    }

    public void setCancellationCutoffHours(Integer cancellationCutoffHours) {
        this.cancellationCutoffHours = cancellationCutoffHours;
    }

    public String getRulesAndRegulations() {
        return rulesAndRegulations;
    }

    public void setRulesAndRegulations(String rulesAndRegulations) {
        this.rulesAndRegulations = rulesAndRegulations;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
