package com.example.voucherservice.request;

import lombok.Data;

@Data
public class VerifyOtpRequest {
    private String requestId;
    private String otp;
}
