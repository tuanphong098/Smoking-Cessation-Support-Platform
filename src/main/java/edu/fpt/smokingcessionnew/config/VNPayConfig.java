package edu.fpt.smokingcessionnew.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Data
public class VNPayConfig {

    @Value("${vnpay.version}")
    private String vnpVersion;

    @Value("${vnpay.command}")
    private String vnpCommand;

    @Value("${vnpay.tmn-code}")
    private String vnpTmnCode;

    @Value("${vnpay.hash-secret}")
    private String vnpHashSecret;

    @Value("${vnpay.currCode}")
    private String vnpCurrCode;

    @Value("${vnpay.locale}")
    private String vnpLocale;

    @Value("${vnpay.return-url}")
    private String vnpReturnUrl;

    @Value("${vnpay.payment-url}")
    private String vnpPayUrl;

    @Value("${vnpay.api-url}")
    private String vnpApiUrl;
}
