package com.example.voucherservice.service;

import com.example.voucherservice.model.Otp;
import com.example.voucherservice.model.OtpState;
import com.example.voucherservice.model.State;
import com.example.voucherservice.model.Voucher;
import com.example.voucherservice.repositories.VoucherRepository;
import com.example.voucherservice.request.GetVoucherRequest;
import com.example.voucherservice.request.SendSmsRequest;
import com.example.voucherservice.response.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Future;

@Service
public class VoucherService {
    private static final Logger LOG = LoggerFactory.getLogger(VoucherService.class);

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private ThirdPartyService thirdPartyService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private OtpService otpService;

    public Voucher getByPurchaseId(String purchaseId) {
        return voucherRepository.findByPurchaseId(purchaseId);
    }


    public Voucher timeout(Voucher voucher) {
        voucher.setState(State.TIMEOUT);
        return voucherRepository.save(voucher);
    }

    public Voucher prepareVoucher(GetVoucherRequest request) {

        validate(request);

        Voucher voucher = new Voucher();
        voucher.setId(UUID.randomUUID());
        voucher.setPhoneNumber(request.getPhoneNumber());
        voucher.setPurchaseId(request.getPurchaseId());
        voucher.setState(State.PENDING);
        return voucherRepository.save(voucher);
    }

    private void validate(GetVoucherRequest request) {
        if (Objects.isNull(request)) {
            throw new IllegalArgumentException("Failed to get voucher, invalid request");
        }
        if (StringUtils.isEmpty(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Failed to get voucher, phone number is required");
        }
        if (StringUtils.isEmpty(request.getPurchaseId())) {
            throw new IllegalArgumentException("Failed to get voucher, purchase id is required");
        }
        Voucher voucher = voucherRepository.findByPurchaseId(request.getPurchaseId());
        if (Objects.nonNull(voucher)) {
            throw new IllegalArgumentException("Failed to get voucher, purchase id is existed");
        }
    }

    public List<Voucher> listVoucher(String phoneNumber, String verificationId) {
        Otp otp = otpService.query(verificationId);
        if (otp.getState() != OtpState.VERIFIED) {
            throw new IllegalArgumentException("Request is not verified yet");
        }
        return voucherRepository.findByPhoneNumber(phoneNumber);
    }

    @Async
    public Future<Voucher> getVoucher(String phoneNumber, String purchaseId, Object lock) {
        LOG.info("Getting voucher");
        GetVoucherRequest getVoucherRequest = new GetVoucherRequest();
        getVoucherRequest.setPhoneNumber(phoneNumber);
        getVoucherRequest.setPurchaseId(purchaseId);
        String voucherCode = thirdPartyService.getVoucher(getVoucherRequest).getVoucherCode();
        synchronized (lock) {
            Voucher voucher = this.voucherRepository.findByPurchaseId(purchaseId);
            if (Objects.isNull(voucher)) {
                throw new IllegalArgumentException("Failed to get voucher, purchase id is existed");
            }
            voucher.setVoucherCode(voucherCode);
            if (voucher.getState() == State.TIMEOUT) {
                LOG.info("Sending voucher to client via SMS");
                SendSmsRequest sendSmsRequest = new SendSmsRequest(phoneNumber, this.prepareSmsMessage(voucherCode, purchaseId));
                BaseResponse response = smsService.sendSms(sendSmsRequest);
                if (!response.isSuccess()) {
                    throw new RestClientException(response.getMessage());
                }
                voucher.setState(State.DELIVERED_BY_SMS);
            } else {
                LOG.info("Sending voucher to client via REST response");
                voucher.setState(State.DELIVERED_BY_REST);
            }
            this.voucherRepository.save(voucher);
            return new AsyncResult<>(voucher);
        }
    }

    private String prepareSmsMessage(String voucherCode, String purchaseId) {
        return String.format("Your voucher of purchase %s is: %s", purchaseId, voucherCode);
    }
}
