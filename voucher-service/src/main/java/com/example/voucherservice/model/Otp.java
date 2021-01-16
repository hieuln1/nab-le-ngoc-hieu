package com.example.voucherservice.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "otp")
public class Otp {
    @Column
    @Id
    private UUID id;

    @Column(name = "phone_number")
    @Getter
    @Setter
    private String phoneNumber;

    @Column(name = "otp")
    @Getter
    @Setter
    private String otp;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private OtpState state;

    @CreationTimestamp
    @Column(name = "created_at")
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant createdAt;

    @Column(name = "expired_at")
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant expiredAt;
}
