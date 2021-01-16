package com.example.voucherservice.service;

import com.example.voucherservice.model.Otp;
import com.example.voucherservice.model.OtpState;
import com.example.voucherservice.repositories.OtpRepository;
import com.example.voucherservice.request.SendSmsRequest;
import com.example.voucherservice.response.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Service
public class OtpService {
    private static final Logger LOG = LoggerFactory.getLogger(OtpService.class);

    @Autowired
    private SmsService smsService;

    @Autowired
    private OtpRepository otpRepository;

    public Otp query(String id) {
        return otpRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification id"));
    }

    public Otp requestOtp(String phoneNumber) {
        Otp otp = new Otp();
        otp.setId(UUID.randomUUID());
        otp.setPhoneNumber(phoneNumber);
        otp.setExpiredAt(Instant.now().plus(3, ChronoUnit.MINUTES));
        String otpStr = generateOtp();
        otp.setOtp(otpStr);

        SendSmsRequest sendSmsRequest = new SendSmsRequest(phoneNumber, prepareSmsMessage(otpStr));
        BaseResponse response = smsService.sendSms(sendSmsRequest);
        if (!response.isSuccess()) {
            throw new RestClientException(response.getMessage());
        }

        return otpRepository.save(otp);
    }

    public BaseResponse verifyOtp(String requestId, String otpStr) {
        Otp otp = otpRepository.findById(UUID.fromString(requestId)).orElse(null);
        if (Objects.isNull(otp)) {
            throw new IllegalArgumentException("Invalid OTP verification request");
        }
        if (otp.getExpiredAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("This OTP has expired");
        }
        if (!otp.getOtp().equals(otpStr)) {
            throw new IllegalArgumentException("Wrong OTP");
        }
        otp.setState(OtpState.VERIFIED);
        otpRepository.save(otp);
        return new BaseResponse(true, "OK");
    }

    private String generateOtp() {
        return new DecimalFormat("000000").format(new SecureRandom().nextInt(999999));
    }

    private String prepareSmsMessage(String otp) {
        return String.format("Your OTP is: %s. It will expire in 3 minutes", otp);
    }
}
