package edu.fpt.smokingcessionnew.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SmokingHistoryResponse {

    private Integer id;
    private Integer userId;
    private String userFullName;
    private Integer cigarettesPerDay;
    private BigDecimal pricePerPack;
    private Integer durationYears;
    private Integer ageStartedSmoking;
    private String brandPreference;
    private String triggers;
    private Integer previousQuitAttempts;
    private Integer longestQuitPeriodDays;
    private Integer motivationLevel;
    private String healthConcerns;
    private String notes;
    private LocalDate recordedDate;

    // Calculated fields for better insights
    private BigDecimal dailyCost;          // Tính toán chi phí hàng ngày
    private BigDecimal monthlyCost;        // Tính toán chi phí hàng tháng
    private BigDecimal yearlyCost;         // Tính toán chi phí hàng năm
    private Integer totalCigarettesSmoked; // Tổng số điếu đã hút (ước tính)
    private String riskLevel;              // Mức độ rủi ro (Low/Medium/High/Very High)
}
