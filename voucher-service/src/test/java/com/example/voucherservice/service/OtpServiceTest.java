package com.example.voucherservice.service;

import com.example.voucherservice.model.Otp;
import com.example.voucherservice.model.Voucher;
import com.example.voucherservice.repositories.OtpRepository;
import com.example.voucherservice.request.SendSmsRequest;
import com.example.voucherservice.response.BaseResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public class OtpServiceTest {
    @InjectMocks
    private OtpService otpService;

    @Mock
    private SmsService smsService;

    @Mock
    private OtpRepository otpRepository;

    @Captor
    private ArgumentCaptor<SendSmsRequest> sendSmsCapture;

    private final String phoneNumber = "phoneNumber";
    private final UUID otpId = UUID.randomUUID();
    private final String otpStr = "123456";

    @BeforeEach
    private void resetContext() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(otpRepository.save(Mockito.any(Otp.class))).thenAnswer(AdditionalAnswers.returnsFirstArg());
    }

    @Test
    public void testRequestOtpSuccess() {
        // Mock
        Mockito.when(smsService.sendSms(ArgumentMatchers.any()))
                .thenReturn(new BaseResponse());

        // Act
        Otp result = otpService.requestOtp(phoneNumber);

        // Assert
        Assertions.assertEquals(result.getPhoneNumber(), phoneNumber);
        Pattern pattern = Pattern.compile("\\d{6}");
        Assertions.assertTrue(pattern.matcher(result.getOtp()).matches());

        Mockito.verify(smsService).sendSms(sendSmsCapture.capture());
        Assertions.assertEquals(sendSmsCapture.getValue().getPhoneNumber(), phoneNumber);
        Assertions.assertEquals(sendSmsCapture.getValue().getMessage(),
                String.format("Your OTP is: %s. It will expire in 3 minutes", result.getOtp()));
    }

    @Test
    public void testVerifyOtpFailByInvalidRequest() {
        // Mock
        Mockito.when(otpRepository.findById(otpId)).thenReturn(Optional.empty());
        // Act & Assert
        Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> otpService.verifyOtp(otpId.toString(), otpStr));
        Assertions.assertEquals(exception.getMessage(), "Invalid OTP verification request");
    }

    @Test
    public void testVerifyOtpFailByOtpExpired() {
        Otp otp = new Otp();
        otp.setExpiredAt(Instant.now().minusSeconds(10));
        // Mock
        Mockito.when(otpRepository.findById(otpId)).thenReturn(Optional.of(otp));
        // Act & Assert
        Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> otpService.verifyOtp(otpId.toString(), otpStr));
        Assertions.assertEquals(exception.getMessage(), "This OTP has expired");
    }

    @Test
    public void testVerifyOtpFailByWrongOtp() {
        Otp otp = new Otp();
        otp.setExpiredAt(Instant.now().plusSeconds(100));
        otp.setOtp(otpStr);
        // Mock
        Mockito.when(otpRepository.findById(otpId)).thenReturn(Optional.of(otp));
        // Act & Assert
        Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> otpService.verifyOtp(otpId.toString(), "invalid_otp"));
        Assertions.assertEquals(exception.getMessage(), "Wrong OTP");
    }

    @Test
    public void testVerifyOtpSuccess() {
        Otp otp = new Otp();
        otp.setExpiredAt(Instant.now().plusSeconds(100));
        otp.setOtp(otpStr);
        // Mock
        Mockito.when(otpRepository.findById(otpId)).thenReturn(Optional.of(otp));
        BaseResponse result = otpService.verifyOtp(otpId.toString(), otpStr);
        Assertions.assertTrue(result.isSuccess());
    }
}
