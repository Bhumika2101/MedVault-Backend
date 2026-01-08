package com.medvault. model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java. util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"appointments", "medicalRecords", "notifications", "feedbacks"})
@ToString(callSuper = true, exclude = {"appointments", "medicalRecords", "notifications", "feedbacks"})
@PrimaryKeyJoinColumn(name = "user_id")
public class Patient extends User {

    private LocalDate dateOfBirth;

    private String gender;

    private String bloodGroup;

    @Column(length = 500)
    private String address;

    @Column(length = 1000)
    private String medicalHistory;

    @Column(length = 1000)
    private String allergies;

    private String emergencyContactName;

    private String emergencyContactPhone;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"patient", "doctor"})
    private List<Appointment> appointments = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("patient")
    private List<MedicalRecord> medicalRecords = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("patient")
    private List<Notification> notifications = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"patient", "doctor", "appointment"})
    private List<Feedback> feedbacks = new ArrayList<>();
}