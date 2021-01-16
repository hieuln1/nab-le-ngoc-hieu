package com.example.voucherservice.service;

import com.example.voucherservice.model.Otp;
import com.example.voucherservice.model.OtpState;
import com.example.voucherservice.model.State;
import com.example.voucherservice.model.Voucher;
import com.example.voucherservice.repositories.VoucherRepository;
import com.example.voucherservice.request.GetVoucherRequest;
import com.example.voucherservice.response.BaseResponse;
import com.example.voucherservice.response.GetVoucherResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.client.RestClientException;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class VoucherServiceTest {

    @InjectMocks
    private VoucherService voucherService;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private ThirdPartyService thirdPartyService;

    @Mock
    private SmsService smsService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private OtpService otpService;

    private final String phoneNumber = "0987654321";
    private final String purchaseId = "11111";
    private final String voucherCode = "voucherCode";

    @BeforeEach
    private void resetContext() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(voucherRepository.save(Mockito.any(Voucher.class))).thenAnswer(AdditionalAnswers.returnsFirstArg());
    }

    @Test
    public void testPrepareVoucher() {
        GetVoucherRequest request = new GetVoucherRequest();
        request.setPurchaseId(purchaseId);
        request.setPhoneNumber(phoneNumber);

        // Act
        Voucher voucher = voucherService.prepareVoucher(request);

        // Assert
        Assertions.assertNotNull(voucher.getId());
        Assertions.assertEquals(voucher.getState(), State.PENDING);
        Assertions.assertEquals(voucher.getPhoneNumber(), request.getPhoneNumber());
        Assertions.assertEquals(voucher.getPurchaseId(), request.getPurchaseId());
    }

    @Test
    public void testPrepareVoucherFailByEmptyRequest() {
        Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,() -> voucherService.prepareVoucher(null));
        Assertions.assertEquals(exception.getMessage(), "Failed to get voucher, invalid request");
    }

    @Test
    public void testPrepareVoucherFailByEmptyPhoneNumber() {
        GetVoucherRequest request = new GetVoucherRequest();
        request.setPurchaseId(purchaseId);
        request.setPhoneNumber(null);
        Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,() -> voucherService.prepareVoucher(request));
        Assertions.assertEquals(exception.getMessage(), "Failed to get voucher, phone number is required");
    }

    @Test
    public void testPrepareVoucherFailByEmptyPurchaseId() {
        GetVoucherRequest request = new GetVoucherRequest();
        request.setPurchaseId(null);
        request.setPhoneNumber(phoneNumber);
        Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,() -> voucherService.prepareVoucher(request));
        Assertions.assertEquals(exception.getMessage(), "Failed to get voucher, purchase id is required");
    }

    @Test
    public void testPrepareVoucherFailByExistedPurchaseId() {
        GetVoucherRequest request = new GetVoucherRequest();
        request.setPurchaseId(purchaseId);
        request.setPhoneNumber(phoneNumber);

        // Mock
        Mockito.when(voucherRepository.findByPurchaseId(purchaseId))
                .thenReturn(new Voucher());

        Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,() -> voucherService.prepareVoucher(request));
        Assertions.assertEquals(exception.getMessage(), "Failed to get voucher, purchase id is existed");
    }

    @Test
    public void testTimeoutVoucher() {
        // Act
        Voucher voucher = voucherService.timeout(new Voucher());

        // Assert
        Assertions.assertEquals(voucher.getState(), State.TIMEOUT);
    }

    @Test
    public void testListVoucherSuccess() {
        String validVerificationId = "validVerificationId";
        Otp verifiedOtp = new Otp();
        verifiedOtp.setState(OtpState.VERIFIED);
        List<Voucher> list = new ArrayList<>();
        list.add(new Voucher());
        list.add(new Voucher());

        // Mock
        Mockito.when(otpService.query(validVerificationId)).thenReturn(verifiedOtp);
        Mockito.when(voucherRepository.findByPhoneNumber(phoneNumber)).thenReturn(list);

        // Act
        List<Voucher> result = voucherService.listVoucher(phoneNumber, validVerificationId);

        // Assert
        Assertions.assertEquals(result.size(), list.size());
    }

    @Test
    public void testListVoucherFail() {
        String invalidVerificationId = "invalidVerificationId";
        Otp unverifiedOtp = new Otp();
        unverifiedOtp.setState(OtpState.PENDING);

        // Mock
        Mockito.when(otpService.query(invalidVerificationId)).thenReturn(unverifiedOtp);

        // Act & Assert
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> voucherService.listVoucher(phoneNumber, invalidVerificationId));
    }

    @Test
    public void testGetVoucherViaRest() throws ExecutionException, InterruptedException {
        Object lock = new Object();
        Voucher voucher = new Voucher();
        voucher.setState(State.PENDING);

        // Mock
        Mockito.when(thirdPartyService.getVoucher(ArgumentMatchers.any()))
                .thenReturn(new GetVoucherResponse(voucherCode));
        Mockito.when(voucherRepository.findByPurchaseId(purchaseId))
                .thenReturn(voucher);

        // Act
        Voucher result = voucherService.getVoucher(phoneNumber, purchaseId, lock).get();

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(result.getVoucherCode(), voucherCode);
        Assertions.assertEquals(result.getState(), State.DELIVERED_BY_REST);
    }

    @Test
    public void testGetVoucherViaSms() throws ExecutionException, InterruptedException {
        Object lock = new Object();
        Voucher voucher = new Voucher();
        voucher.setState(State.TIMEOUT);

        // Mock
        Mockito.when(thirdPartyService.getVoucher(ArgumentMatchers.any()))
                .thenReturn(new GetVoucherResponse(voucherCode));
        Mockito.when(voucherRepository.findByPurchaseId(purchaseId))
                .thenReturn(voucher);
        Mockito.when(smsService.sendSms(ArgumentMatchers.any()))
                .thenReturn(new BaseResponse());

        // Act
        Voucher result = voucherService.getVoucher(phoneNumber, purchaseId, lock).get();

        // Assert
        Assertions.assertNotNull(result);
        Assertions.assertEquals(result.getVoucherCode(), voucherCode);
        Assertions.assertEquals(result.getState(), State.DELIVERED_BY_SMS);
    }

    @Test
    public void testGetVoucherFailBySms() throws ExecutionException, InterruptedException {
        Object lock = new Object();
        Voucher voucher = new Voucher();
        voucher.setState(State.TIMEOUT);

        // Mock
        Mockito.when(thirdPartyService.getVoucher(ArgumentMatchers.any()))
                .thenReturn(new GetVoucherResponse(voucherCode));
        Mockito.when(voucherRepository.findByPurchaseId(purchaseId))
                .thenReturn(voucher);
        Mockito.when(smsService.sendSms(ArgumentMatchers.any()))
                .thenReturn(new BaseResponse(false, "ERROR"));

        // Act
        Assertions.assertThrows(RestClientException.class,
                () -> voucherService.getVoucher(phoneNumber, purchaseId, lock));
    }
}
