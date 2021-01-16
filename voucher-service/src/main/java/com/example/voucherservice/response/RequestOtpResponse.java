package com.example.voucherservice.response;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestOtpResponse extends BaseResponse {
    private String otpRequestId;
}
