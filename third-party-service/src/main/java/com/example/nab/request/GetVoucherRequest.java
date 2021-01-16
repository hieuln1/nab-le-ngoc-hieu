package com.example.nab.request;

import lombok.Data;

@Data
public class GetVoucherRequest {
    private String phoneNumber;
    private String purchaseId;
}
