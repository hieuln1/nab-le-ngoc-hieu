CREATE TABLE voucher (
    id character varying(255) PRIMARY KEY,
    phone_number character varying(255),
    purchase_id character varying(255) UNIQUE,
    voucher_code character varying(255),
    state character varying(255),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);

CREATE TABLE otp (
    id character varying(255) PRIMARY KEY,
    phone_number character varying(255),
    otp character varying(255),
    state character varying(255),
    created_at timestamp without time zone,
    expired_at timestamp without time zone
);