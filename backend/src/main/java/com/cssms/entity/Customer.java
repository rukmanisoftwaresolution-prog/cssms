package com.cssms.entity;

import com.cssms.enums.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@Entity
@Table(name = "customers")
@SQLRestriction("deleted = false")
public class Customer extends BaseEntity {

    @Column(name = "customer_code", nullable = false, length = 20)
    private String customerCode;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "company_name", length = 150)
    private String companyName;

    @Column(name = "gst_number", length = 15)
    private String gstNumber;

    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    @Column(length = 150)
    private String email;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(name = "credit_limit", nullable = false, precision = 14, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "outstanding_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal outstandingAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Status status = Status.ACTIVE;
}
