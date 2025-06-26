package edu.fpt.smokingcessionnew.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse implements Serializable {
    private String vnpTxnRef;
    private String vnpAmount;
    private String vnpOrderInfo;
    private String vnpResponseCode;
    private String vnpTransactionNo;
    private String vnpBankCode;
    private String vnpCardType;
    private String vnpPayDate;
    private String vnpTransactionStatus;
    private String vnpSecureHash;
}
