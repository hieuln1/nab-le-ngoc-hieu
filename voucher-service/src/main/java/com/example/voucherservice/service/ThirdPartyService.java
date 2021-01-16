package com.example.voucherservice.service;

import com.example.voucherservice.request.GetVoucherRequest;
import com.example.voucherservice.request.SendSmsRequest;
import com.example.voucherservice.response.BaseResponse;
import com.example.voucherservice.response.GetVoucherResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ThirdPartyService {
    private static final Logger LOG = LoggerFactory.getLogger(ThirdPartyService.class);

    @Value("${application.third-party.api.voucher}")
    private String voucherApi;

    @Value("${application.third-party.api.ping}")
    private String pingApi;

    private RestTemplate restTemplate = new RestTemplate();

    public BaseResponse testConnection() {
        String uri = UriComponentsBuilder.fromHttpUrl(pingApi).toUriString();
        LOG.info("Calling API: {}", pingApi);
        LOG.info("Method: {}", HttpMethod.GET);
        ResponseEntity<BaseResponse> response = restTemplate.exchange(uri, HttpMethod.GET, null, BaseResponse.class);
        LOG.info(response.toString());
        if (response.getStatusCode() != HttpStatus.OK) {
            LOG.error("Fail to send SMS message, status code {}", response.getStatusCodeValue());
            throw new RestClientException(String.format("Fail to send SMS message, status code %s", response.getStatusCodeValue()));
        } else {
            LOG.info("OK");
        }
        return new BaseResponse();
    }

    public GetVoucherResponse getVoucher(GetVoucherRequest request) {
        String uri = UriComponentsBuilder.fromHttpUrl(voucherApi).toUriString();
        HttpEntity httpEntity = new HttpEntity(request);
        LOG.info("Calling API: {}", voucherApi);
        ResponseEntity<GetVoucherResponse> response = restTemplate.exchange(uri, HttpMethod.POST, httpEntity, GetVoucherResponse.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            LOG.error("Fail to get voucher from third party, status code {}", response.getStatusCodeValue());
            throw new RestClientException(String.format("Fail to get voucher from third party, status code %s", response.getStatusCodeValue()));
        }
        return response.getBody();
    }
}
