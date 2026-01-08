package com.medvault.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DoctorCreationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phoneNumber;

    @NotBlank(message = "Specialization is required")
    private String specialization;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @NotBlank(message = "Qualification is required")
    private String qualification;

    @Min(value = 0, message = "Experience years cannot be negative")
    private Integer experienceYears;

    private String bio;
    private String hospitalAffiliation;

    @NotNull(message = "Consultation fee is required")
    @DecimalMin(value = "0.01", message = "Consultation fee must be greater than 0")
    private Double consultationFee;

    private String availableTimings;
}