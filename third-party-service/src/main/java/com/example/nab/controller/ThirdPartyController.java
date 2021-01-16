package com.example.nab.controller;

import com.example.nab.request.GetVoucherRequest;
import com.example.nab.response.GetVoucherResponse;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;

import java.util.Random;
import java.util.UUID;

@RestController
public class ThirdPartyController {
    private static Logger LOG = LoggerFactory.getLogger(ThirdPartyController.class);

    @GetMapping("/ping")
    public String ping() {
        return "Third party service ok";
    }

    @PostMapping(value = "/voucher", consumes = MediaType.APPLICATION_JSON_VALUE)
    public GetVoucherResponse sendSms(@RequestBody GetVoucherRequest request) {
        LOG.info("Received get voucher request to phone number {} with purchase id {}", request.getPhoneNumber(), request.getPurchaseId());
        // Random proccess time from 3 to 130s
        Random rd = new Random();
        int timeToProcess = rd.nextInt(127) + 3;
        LOG.info("Simulate processing in {} seconds", timeToProcess);
        try {
            Thread.sleep(timeToProcess * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        GetVoucherResponse response = new GetVoucherResponse();
        response.setVoucherCode(UUID.randomUUID().toString());
        LOG.info("Process generate voucher finished");
        return response;
    }

}
