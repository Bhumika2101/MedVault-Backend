package com.medvault.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "doctors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = { "appointments", "feedbacks" })
@ToString(callSuper = true, exclude = { "appointments", "feedbacks" })
@PrimaryKeyJoinColumn(name = "user_id")
public class Doctor extends User {

    @Column(nullable = false)
    private String specialization;

    @Column(nullable = false, unique = true)
    private String licenseNumber;

    @Column(nullable = false)
    private String qualification;

    private Integer experienceYears;

    @Column(length = 1000)
    private String bio;

    private String hospitalAffiliation;

    @Column(nullable = false)
    private Double consultationFee;

    @Column(length = 1000)
    private String availableTimings;

    @Column(nullable = false)
    private Boolean isAvailable = true;

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({ "patient", "doctor" }) // Prevents infinite recursion
    private List<Appointment> appointments = new ArrayList<>();

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("doctor") // Prevents infinite recursion
    private List<Feedback> feedbacks = new ArrayList<>();
}