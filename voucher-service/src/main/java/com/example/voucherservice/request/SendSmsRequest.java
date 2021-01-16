package com.example.voucherservice.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SendSmsRequest {
    private String phoneNumber;
    private String message;
}
