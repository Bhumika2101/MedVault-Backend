package com.medvault.controller;

import com.medvault.dto.request.PaymentVerificationRequest;
import com.medvault.dto.response.ApiResponse;
import com.medvault.dto.response.PaymentResponse;
import com.medvault.dto.response.RevenueResponse;
import com.medvault.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order/{appointmentId}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPaymentOrder(@PathVariable Long appointmentId) {
        PaymentResponse response = paymentService.createPaymentOrder(appointmentId);
        return ResponseEntity.ok(ApiResponse.success("Payment order created", response));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request) {
        PaymentResponse response = paymentService.verifyAndCompletePayment(request);
        return ResponseEntity.ok(ApiResponse.success("Payment verified", response));
    }

    @GetMapping("/doctor-revenue")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<RevenueResponse>> getDoctorRevenue(@RequestParam Long doctorId) {
        RevenueResponse response = paymentService.getDoctorRevenue(doctorId);
        return ResponseEntity.ok(ApiResponse.success("Doctor revenue retrieved", response));
    }

    @GetMapping("/total-revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RevenueResponse>> getTotalRevenue() {
        RevenueResponse response = paymentService.getTotalRevenue();
        return ResponseEntity.ok(ApiResponse.success("Total revenue retrieved", response));
    }
}
