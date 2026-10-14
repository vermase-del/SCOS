package com.scos.modules.community.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommunityRegistrationRequest {

    // --- Community Details ---
    @NotBlank(message = "Community name is required")
    @Size(min = 3, max = 120, message = "Community name must be between 3 and 120 characters")
    private String name;

    private String code;

    private String legalName;

    private String registrationNumber;

    private String taxIdentifier;

    @NotBlank(message = "Address is required")
    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State/Province is required")
    private String stateProvince;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    private String countryCode = "IND";

    private String phone;

    @NotBlank(message = "Society contact email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    private String currencyCode = "INR";

    private String timezone = "Asia/Kolkata";

    // --- Primary Administrator Account ---
    @NotBlank(message = "Admin first name is required")
    private String adminFirstName;

    @NotBlank(message = "Admin last name is required")
    private String adminLastName;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Must be a valid admin email address")
    private String adminEmail;

    @NotBlank(message = "Admin phone number is required")
    private String adminPhone;

    @NotBlank(message = "Admin username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String adminUsername;

    @NotBlank(message = "Admin password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String adminPassword;

    // --- Optional Rapid Infrastructure Provisioning ---
    private int numberOfTowers = 0;
    private int floorsPerTower = 0;
    private int unitsPerFloor = 0;
    private String towerPrefix = "Tower ";

    public CommunityRegistrationRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

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

    public String getAdminFirstName() { return adminFirstName; }
    public void setAdminFirstName(String adminFirstName) { this.adminFirstName = adminFirstName; }

    public String getAdminLastName() { return adminLastName; }
    public void setAdminLastName(String adminLastName) { this.adminLastName = adminLastName; }

    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }

    public String getAdminPhone() { return adminPhone; }
    public void setAdminPhone(String adminPhone) { this.adminPhone = adminPhone; }

    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }

    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }

    public int getNumberOfTowers() { return numberOfTowers; }
    public void setNumberOfTowers(int numberOfTowers) { this.numberOfTowers = numberOfTowers; }

    public int getFloorsPerTower() { return floorsPerTower; }
    public void setFloorsPerTower(int floorsPerTower) { this.floorsPerTower = floorsPerTower; }

    public int getUnitsPerFloor() { return unitsPerFloor; }
    public void setUnitsPerFloor(int unitsPerFloor) { this.unitsPerFloor = unitsPerFloor; }

    public String getTowerPrefix() { return towerPrefix; }
    public void setTowerPrefix(String towerPrefix) { this.towerPrefix = towerPrefix; }
}
