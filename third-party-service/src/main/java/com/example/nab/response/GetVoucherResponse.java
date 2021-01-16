package com.example.nab.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class GetVoucherResponse {
    private boolean success;
    private String message;
    private String voucherCode;
}
