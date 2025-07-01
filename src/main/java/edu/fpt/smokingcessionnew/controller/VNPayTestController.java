package edu.fpt.smokingcessionnew.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller để phục vụ các trang test VNPay
 */
@Controller
@RequestMapping("/test")
public class VNPayTestController {

    @GetMapping("/vnpay")
    public String vnpayTestPage() {
        return "forward:/vnpay-test-simple.html";
    }

    @GetMapping("/payment")
    public String paymentTestPage() {
        return "forward:/vnpay-test-simple.html";
    }
}
