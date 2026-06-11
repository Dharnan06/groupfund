package com.nammakuzhu.paymentModule.controller;

import com.nammakuzhu.paymentModule.dto.PaymentRequest;
import com.nammakuzhu.paymentModule.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<?> listGroupPayments(
            @PathVariable Long groupId,
            @RequestParam(required = false) LocalDate paymentMonth
    ) {
        return paymentService.listGroupPayments(groupId, paymentMonth);
    }

    @GetMapping("/group/{groupId}/summary")
    public ResponseEntity<?> listGroupPaymentSummary(
            @PathVariable Long groupId,
            @RequestParam(required = false) LocalDate paymentMonth
    ) {
        return paymentService.listGroupPaymentSummary(groupId, paymentMonth);
    }

    @GetMapping("/group/{groupId}/my-history")
    public ResponseEntity<?> listMyGroupPayments(@PathVariable Long groupId) {
        return paymentService.listMyGroupPayments(groupId);
    }

    @PostMapping("/record")
    public ResponseEntity<?> recordPayment(@RequestBody PaymentRequest request) {
        return paymentService.recordPayment(request);
    }
}
