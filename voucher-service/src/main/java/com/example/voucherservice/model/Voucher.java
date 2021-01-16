package com.example.voucherservice.model;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "voucher")
public class Voucher {
    @Column
    @Id
    private UUID id;

    @Column(name = "phone_number")
    @Getter
    @Setter
    private String phoneNumber;

    @Column(name = "purchase_id", unique = true)
    private String purchaseId;

    @Column(name = "voucher_code")
    private String voucherCode;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private State state;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Override
    public String toString() {
        return "Voucher{" +
                "id=" + id +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", purchaseId='" + purchaseId + '\'' +
                ", voucherCode='" + voucherCode + '\'' +
                ", state=" + state +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
