package com.example.voucherservice.repositories;

import com.example.voucherservice.model.Otp;
import com.example.voucherservice.model.Voucher;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface OtpRepository extends CrudRepository<Otp, UUID> {
}
