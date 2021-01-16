package com.example.voucherservice.controller;

import com.example.voucherservice.model.Otp;
import com.example.voucherservice.request.VerifyOtpRequest;
import com.example.voucherservice.response.BaseResponse;
import com.example.voucherservice.response.RequestOtpResponse;
import com.example.voucherservice.service.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/otp")
public class OtpController {
    private static final Logger LOG = LoggerFactory.getLogger(OtpController.class);

    @Autowired
    private OtpService otpService;

    @PostMapping("/request")
    public RequestOtpResponse requestOtp(@RequestParam("phone_number") String phoneNumber) {
        LOG.info("Requesting OTP for phone number {}", phoneNumber);
        Otp otp = otpService.requestOtp(phoneNumber);
        RequestOtpResponse response = new RequestOtpResponse();
        response.setOtpRequestId(otp.getId().toString());
        return response;
    }

    @PostMapping("/verify")
    public BaseResponse verifyOtp(@RequestBody VerifyOtpRequest request) {
        LOG.info("Verify OTP for request {}", request.getRequestId());
        return otpService.verifyOtp(request.getRequestId(), request.getOtp());
    }

    // Since we cannot get OTP via SMS then provide this endpoint to get OTP to verify in test
    @GetMapping("/{otp_id}")
    public String getOtp(@PathVariable("otp_id") String id) {
        return otpService.query(id).getOtp();
    }
}
