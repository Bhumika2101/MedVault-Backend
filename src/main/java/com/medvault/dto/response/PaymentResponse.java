package com.medvault.dto.response;

import com.medvault.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long appointmentId;
    private Double amount;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    // Patient and Doctor details for revenue display
    private String patientName;
    private Long patientId;
    private String doctorName;
}
