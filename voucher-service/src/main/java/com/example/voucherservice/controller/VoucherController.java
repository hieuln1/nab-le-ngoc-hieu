package com.example.voucherservice.controller;

import com.example.voucherservice.model.Voucher;
import com.example.voucherservice.request.GetVoucherRequest;
import com.example.voucherservice.response.GetVoucherResponse;
import com.example.voucherservice.service.VoucherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@RestController
public class VoucherController {

    private static final Logger LOG = LoggerFactory.getLogger(VoucherController.class);

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private VoucherService voucherService;

    @GetMapping("/ping")
    public String ping() {
        return "Voucher service ok";
    }

    @GetMapping("/voucher")
    public List<String> getListVoucher(@RequestParam("phone_number") String phoneNumber,
                                        @RequestParam("verification_id") String verificationId) {
        return voucherService.listVoucher(phoneNumber, verificationId).stream()
                .map(Voucher::getVoucherCode).collect(Collectors.toList());
    }

    @PostMapping(value = "/voucher", consumes = MediaType.APPLICATION_JSON_VALUE)
    public GetVoucherResponse getVoucher(@RequestBody GetVoucherRequest request)
            throws InterruptedException, ExecutionException {

        Object lock = new Object();
        Voucher voucher = voucherService.prepareVoucher(request);

        Future<Voucher> futureResult = voucherService.getVoucher(voucher.getPhoneNumber(), voucher.getPurchaseId(), lock);
        try {
            futureResult.get(30, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e;
        } catch (TimeoutException ignored) {}

        synchronized (lock) {
            entityManager.clear();
            Voucher checkVoucher = voucherService.getByPurchaseId(request.getPurchaseId());
            if (StringUtils.isEmpty(checkVoucher.getVoucherCode())) {
                LOG.info("Cannot get voucher code within 30s, voucher will be sent via SMS later");
                voucherService.timeout(checkVoucher);
                GetVoucherResponse response = new GetVoucherResponse();
                response.setMessage("The request is being processed, voucher will be sent via SMS later");
                return response;
            }
            return new GetVoucherResponse(checkVoucher.getVoucherCode());
        }
    }
}
