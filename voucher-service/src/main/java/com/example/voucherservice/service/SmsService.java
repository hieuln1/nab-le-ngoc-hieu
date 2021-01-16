package com.example.voucherservice.service;

import com.example.voucherservice.request.SendSmsRequest;
import com.example.voucherservice.response.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsService {
    private static final Logger LOG = LoggerFactory.getLogger(SmsService.class);

    public BaseResponse sendSms(SendSmsRequest request) {
        LOG.info("Mock sending SMS to phone number {} with message {}", request.getPhoneNumber(), request.getMessage());
        return new BaseResponse();
    }

}
