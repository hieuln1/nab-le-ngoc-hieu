package com.example.voucherservice.repositories;

import com.example.voucherservice.model.Voucher;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface VoucherRepository extends CrudRepository<Voucher, UUID> {
    public Voucher findByPurchaseId(String purchaseId);

    public List<Voucher> findByPhoneNumber(String phoneNumber);
}
