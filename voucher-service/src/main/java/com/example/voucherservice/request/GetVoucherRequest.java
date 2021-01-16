package com.example.voucherservice.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetVoucherRequest {
    private String phoneNumber;
    private String purchaseId;
}
