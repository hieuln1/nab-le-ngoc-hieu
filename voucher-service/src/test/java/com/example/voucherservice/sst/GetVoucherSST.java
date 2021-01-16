package com.example.voucherservice.sst;

import com.example.voucherservice.VoucherServiceApplication;
import com.example.voucherservice.model.Otp;
import com.example.voucherservice.model.Voucher;
import com.example.voucherservice.repositories.OtpRepository;
import com.example.voucherservice.repositories.VoucherRepository;
import com.example.voucherservice.response.BaseResponse;
import com.example.voucherservice.response.GetVoucherResponse;
import com.example.voucherservice.response.RequestOtpResponse;
import com.example.voucherservice.service.ThirdPartyService;
import com.example.voucherservice.service.VoucherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import javax.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@SpringBootTest(classes = VoucherServiceApplication.class)
@AutoConfigureEmbeddedDatabase
@AutoConfigureMockMvc
public class GetVoucherSST {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @MockBean
    private ThirdPartyService thirdPartyService;

    @Captor
    private ArgumentCaptor<Otp> otpArgumentCaptor;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    private void resetContext() {
        MockitoAnnotations.initMocks(this);
        otpRepository.deleteAll();
        voucherRepository.deleteAll();
    }

    private final String phoneNumber = "0987654321";
    private final String purchaseId = "1111111111";
    private final String voucherCode = "validVoucherCode";

    @Test
    public void testGetVoucherSuccessViaREST() throws Exception {
        // PURCHASE AND GET VOUCHER SUCCESS VIA SMS
        // Mock
        mockThirdPartyServiceResponseVoucher(5);

        // Act
        MvcResult queryResult = mvc.perform(MockMvcRequestBuilders.post("/voucher")
                .content(String.format("{\"phoneNumber\":\"%s\",\"purchaseId\":\"%s\"}", phoneNumber, purchaseId))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        GetVoucherResponse response = objectMapper.readValue(queryResult.getResponse().getContentAsString(), GetVoucherResponse.class);

        // Assert
        Assertions.assertNotNull(response.getVoucherCode());

        testGetVoucherList();
    }

    @Test
    public void testGetVoucherSuccessViaSMS() throws Exception {
        // PURCHASE AND GET VOUCHER SUCCESS VIA SMS
        // Mock
        mockThirdPartyServiceResponseVoucher(35);

        // Act
        MvcResult queryResult = mvc.perform(MockMvcRequestBuilders.post("/voucher")
                .content(String.format("{\"phoneNumber\":\"%s\",\"purchaseId\":\"%s\"}", phoneNumber, purchaseId))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        GetVoucherResponse response = objectMapper.readValue(queryResult.getResponse().getContentAsString(), GetVoucherResponse.class);

        // Assert
        Assertions.assertNull(response.getVoucherCode());
        Assertions.assertEquals(response.getMessage(), "The request is being processed, voucher will be sent via SMS later");

        Thread.sleep(6000);

        testGetVoucherList();
    }

    private void testGetVoucherList() throws Exception {
        // REQUEST OTP TO VIEW VOUCHER LIST
        //Act
        MvcResult requestOtpResult = mvc.perform(MockMvcRequestBuilders.post("/otp/request")
                .param("phone_number", phoneNumber)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        RequestOtpResponse requestOtpResponse = objectMapper.readValue(requestOtpResult.getResponse().getContentAsString(), RequestOtpResponse.class);
        String otpRequestId = requestOtpResponse.getOtpRequestId();
        // Assert
        Assertions.assertTrue(requestOtpResponse.isSuccess());
        Assertions.assertNotNull(otpRequestId);

        // GET OTP
        // Since we cannot get OTP via SMS then use this endpoint to get OTP to verify in test
        MvcResult otpResult = mvc.perform(MockMvcRequestBuilders.get("/otp/" + otpRequestId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        String otpStr = otpResult.getResponse().getContentAsString();

        // VERIFY OTP
        //Act
        MvcResult verifyOtpResult = mvc.perform(MockMvcRequestBuilders.post("/otp/verify")
                .content(String.format("{\"requestId\":\"%s\",\"otp\":\"%s\"}", otpRequestId, otpStr))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        BaseResponse verifyOtpResponse = objectMapper.readValue(verifyOtpResult.getResponse().getContentAsString(), BaseResponse.class);

        // Assert
        Assertions.assertTrue(verifyOtpResponse.isSuccess());

        // GET LIST VOUCHER AFTER VERIFY OTP
        MvcResult listVoucherResult = mvc.perform(MockMvcRequestBuilders.get("/voucher")
                .param("phone_number", phoneNumber)
                .param("verification_id", otpRequestId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        List listResult = objectMapper.readValue(listVoucherResult.getResponse().getContentAsString(), List.class);

        // Assert
        Assertions.assertEquals(listResult.size(), 1);
        Assertions.assertEquals(listResult.get(0), voucherCode);
    }

    private void mockThirdPartyServiceResponseVoucher(int delay) {
        Mockito.when(thirdPartyService.getVoucher(ArgumentMatchers.any()))
                .thenAnswer((Answer<GetVoucherResponse>) invocation -> {
                    Thread.sleep(delay * 1000);
                    return new GetVoucherResponse(voucherCode);
                });
    }
}
