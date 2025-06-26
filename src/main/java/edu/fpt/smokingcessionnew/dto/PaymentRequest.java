package edu.fpt.smokingcessionnew.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private Integer userId;
    private Integer packageId;
    private String orderInfo;
    private BigDecimal amount;
    private String bankCode;
    private String language;
    private String ipAddress;
}
