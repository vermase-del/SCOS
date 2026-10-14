package com.scos.modules.community.entity;

import com.scos.common.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "scos_communities")
public class CommunityEntity extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "legal_name", length = 150)
    private String legalName;

    @Column(name = "registration_number", length = 60)
    private String registrationNumber;

    @Column(name = "tax_identifier", length = 40)
    private String taxIdentifier;

    @Column(name = "address_line1", nullable = false, length = 150)
    private String addressLine1;

    @Column(name = "address_line2", length = 150)
    private String addressLine2;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(name = "state_province", nullable = false, length = 80)
    private String stateProvince;

    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @Column(name = "country_code", nullable = false, length = 3)
    private String countryCode = "IND";

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "INR";

    @Column(nullable = false, length = 50)
    private String timezone = "Asia/Kolkata";

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    public CommunityEntity() {}

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getTaxIdentifier() { return taxIdentifier; }
    public void setTaxIdentifier(String taxIdentifier) { this.taxIdentifier = taxIdentifier; }

    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getStateProvince() { return stateProvince; }
    public void setStateProvince(String stateProvince) { this.stateProvince = stateProvince; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
