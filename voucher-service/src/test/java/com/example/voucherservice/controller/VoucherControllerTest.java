package com.example.voucherservice.controller;

import com.example.voucherservice.model.State;
import com.example.voucherservice.model.Voucher;
import com.example.voucherservice.request.GetVoucherRequest;
import com.example.voucherservice.response.GetVoucherResponse;
import com.example.voucherservice.service.ThirdPartyService;
import com.example.voucherservice.service.VoucherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import sun.util.resources.cldr.nyn.CalendarData_nyn_UG;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@ExtendWith(SpringExtension.class)
@WebMvcTest(VoucherController.class)
@AutoConfigureMockMvc
public class VoucherControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private EntityManager entityManagerMock;

    @MockBean
    private VoucherService voucherServiceMock;

    private final String phoneNumber = "0987654321";
    private final String purchaseId = "1111111111";

    @BeforeEach
    private void resetContext() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetListVoucher() throws Exception {
        // Mock
        mockGetListVoucher();

        // Act
        MvcResult queryResult = mvc.perform(MockMvcRequestBuilders.get("/voucher")
                .param("phone_number", phoneNumber)
                .param("verification_id", "123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List response = objectMapper.readValue(queryResult.getResponse().getContentAsString(), List.class);

        // Assert
        Assertions.assertEquals(response.size(), 2);
    }

    @Test
    public void testGetVoucherSuccess() throws Exception {
        // Mock
        mockPrepareVoucher();
        mockGetVoucherInTime();

        // Act
        MvcResult queryResult = mvc.perform(MockMvcRequestBuilders.post("/voucher")
                    .content(String.format("{\"phoneNumber\":\"%s\",\"purchaseId\":\"%s\"}", phoneNumber, purchaseId))
                    .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        GetVoucherResponse response = objectMapper.readValue(queryResult.getResponse().getContentAsString(), GetVoucherResponse.class);

        // Assert
        Assertions.assertNotNull(response.getVoucherCode());
    }

    @Test
    public void testGetVoucherTimeout() throws Exception {
        // Mock
        mockPrepareVoucher();
        mockGetVoucherTimeout();

        // Act
        MvcResult queryResult = mvc.perform(MockMvcRequestBuilders.post("/voucher")
                .content(String.format("{\"phoneNumber\":\"%s\",\"purchaseId\":\"%s\"}", phoneNumber, purchaseId))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        GetVoucherResponse response = objectMapper.readValue(queryResult.getResponse().getContentAsString(), GetVoucherResponse.class);

        // Assert
        Assertions.assertEquals(response.getMessage(), "The request is being processed, voucher will be sent via SMS later");
    }

    private void mockThirdPartyServiceResponseVoucher() {
        Mockito.when(voucherServiceMock.getVoucher(ArgumentMatchers.any(),ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenAnswer((Answer<Future<Voucher>>) invocation -> CompletableFuture.completedFuture(new Voucher()));
    }

    private void mockPrepareVoucher() {
        Mockito.when(voucherServiceMock.prepareVoucher(ArgumentMatchers.any())).thenReturn(new Voucher());
    }

    private void mockGetVoucherInTime() {
        mockThirdPartyServiceResponseVoucher();
        Voucher voucher = new Voucher();
        voucher.setVoucherCode(UUID.randomUUID().toString());
        Mockito.when(voucherServiceMock.getByPurchaseId(ArgumentMatchers.any())).thenReturn(voucher);
    }

    private void mockGetVoucherTimeout() {
        mockThirdPartyServiceResponseVoucher();
        Voucher voucher = new Voucher();
        Mockito.when(voucherServiceMock.getByPurchaseId(ArgumentMatchers.any())).thenReturn(voucher);
    }

    private void mockGetListVoucher() {
        List<Voucher> list = new ArrayList<>();
        list.add(new Voucher());
        list.add(new Voucher());
        Mockito.when(voucherServiceMock.listVoucher(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn(list);
    }
}
