package com.example.voucherservice.response;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class GetVoucherResponse extends BaseResponse {
    private String voucherCode;

    public GetVoucherResponse(String voucher) {
        this.voucherCode = voucher;
    }
}
