package com.medvault.service;

import com.medvault.dto.request.PaymentVerificationRequest;
import com.medvault.dto.response.PaymentResponse;
import com.medvault.dto.response.RevenueResponse;
import com.medvault.exception.ResourceNotFoundException;
import com.medvault.model.Appointment;
import com.medvault.model.Payment;
import com.medvault.model.enums.PaymentStatus;
import com.medvault.repository.AppointmentRepository;
import com.medvault.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AppointmentRepository appointmentRepository;
    private final EmailService emailService;

    // Razorpay credentials
    @Value("${razorpay.key.id:rzp_test_Q13Kh5Own98eXP}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:}")
    private String razorpayKeySecret;

    @Transactional
    public PaymentResponse createPaymentOrder(Long appointmentId) {
        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

            // Check if payment already exists
            if (paymentRepository.findByAppointmentId(appointmentId).isPresent()) {
                throw new IllegalStateException("Payment already exists for this appointment");
            }

            // Validate consultation fee
            Double consultationFee = appointment.getDoctor().getConsultationFee();
            if (consultationFee == null || consultationFee < 1.0) {
                throw new IllegalStateException(
                        "Consultation fee must be at least ₹1. Please set a valid consultation fee for the doctor.");
            }

            // Create Razorpay client
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            // Create Razorpay order
            // Amount must be in paise (smallest currency unit) and at least 100 paise (₹1)
            int amountInPaise = (int) Math.round(consultationFee * 100);
            if (amountInPaise < 100) {
                throw new IllegalStateException("Order amount must be at least ₹1 (100 paise)");
            }

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise); // Amount in paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "receipt_" + appointmentId);

            log.info("Creating Razorpay order for appointment {} with amount: ₹{} ({} paise)",
                    appointmentId, consultationFee, amountInPaise);

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String orderId = razorpayOrder.get("id");

            log.info("Razorpay order created successfully: {}", orderId);

            // Save payment in database
            Payment payment = Payment.builder()
                    .appointment(appointment)
                    .amount(consultationFee)
                    .razorpayOrderId(orderId)
                    .status(PaymentStatus.PENDING)
                    .build();

            Payment savedPayment = paymentRepository.save(payment);

            return mapToPaymentResponse(savedPayment);
        } catch (RazorpayException e) {
            log.error("Error creating Razorpay order", e);
            throw new RuntimeException("Failed to create payment order: " + e.getMessage());
        }
    }

    @Transactional
    public PaymentResponse verifyAndCompletePayment(PaymentVerificationRequest request) {
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // Verify signature using Razorpay utility
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);

            if (isValid) {
                payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
                payment.setRazorpaySignature(request.getRazorpaySignature());
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setPaidAt(LocalDateTime.now());

                // Send payment confirmation email to patient
                try {
                    Appointment appointment = payment.getAppointment();
                    String patientName = appointment.getPatient().getFirstName() + " " +
                            appointment.getPatient().getLastName();
                    String doctorName = "Dr. " + appointment.getDoctor().getFirstName() + " " +
                            appointment.getDoctor().getLastName();
                    emailService.sendPaymentConfirmationEmail(
                            appointment.getPatient().getEmail(),
                            patientName,
                            doctorName,
                            payment.getAmount(),
                            appointment.getAppointmentDateTime().toString());
                    log.info("✅ Payment confirmation email sent to patient");
                } catch (Exception e) {
                    log.error("Failed to send payment confirmation email", e);
                    // Don't fail payment if email fails
                }
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Invalid signature");
            }

            Payment updatedPayment = paymentRepository.save(payment);
            return mapToPaymentResponse(updatedPayment);
        } catch (RazorpayException e) {
            log.error("Error verifying payment signature", e);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Signature verification failed");
            paymentRepository.save(payment);
            throw new RuntimeException("Payment verification failed: " + e.getMessage());
        }
    }

    public RevenueResponse getDoctorRevenue(Long doctorId) {
        Double totalRevenue = paymentRepository.getDoctorRevenue(doctorId);
        List<Payment> payments = paymentRepository.getDoctorPayments(doctorId);

        List<PaymentResponse> recentPayments = payments.stream()
                .limit(10)
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());

        return RevenueResponse.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : 0.0)
                .totalCompletedPayments(payments.size())
                .averageConsultationFee(payments.isEmpty() ? 0.0 : totalRevenue / payments.size())
                .recentPayments(recentPayments)
                .build();
    }

    public RevenueResponse getTotalRevenue() {
        Double totalRevenue = paymentRepository.getTotalRevenue();
        List<Payment> allPayments = paymentRepository.findByStatus(PaymentStatus.COMPLETED);

        List<PaymentResponse> recentPayments = allPayments.stream()
                .sorted((p1, p2) -> p2.getPaidAt().compareTo(p1.getPaidAt()))
                .limit(20)
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());

        return RevenueResponse.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : 0.0)
                .totalCompletedPayments(allPayments.size())
                .averageConsultationFee(allPayments.isEmpty() ? 0.0 : totalRevenue / allPayments.size())
                .recentPayments(recentPayments)
                .build();
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        Appointment appointment = payment.getAppointment();
        String patientName = appointment.getPatient().getFirstName() + " " +
                appointment.getPatient().getLastName();
        String doctorName = "Dr. " + appointment.getDoctor().getFirstName() + " " +
                appointment.getDoctor().getLastName();

        return PaymentResponse.builder()
                .id(payment.getId())
                .appointmentId(appointment.getId())
                .amount(payment.getAmount())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .patientName(patientName)
                .patientId(appointment.getPatient().getId())
                .doctorName(doctorName)
                .build();
    }
}
