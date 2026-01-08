package com.medvault.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueResponse {
    private Double totalRevenue;
    private Integer totalCompletedPayments;
    private Double averageConsultationFee;
    private List<PaymentResponse> recentPayments;
}
