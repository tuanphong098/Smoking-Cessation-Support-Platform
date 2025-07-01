package edu.fpt.smokingcessionnew.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SmokingHistoryRequest {

    @NotNull(message = "Số điếu thuốc mỗi ngày không được để trống")
    @Min(value = 1, message = "Số điếu thuốc mỗi ngày phải lớn hơn 0")
    private Integer cigarettesPerDay;

    @NotNull(message = "Giá tiền mỗi gói thuốc không được để trống")
    @Min(value = 0, message = "Giá tiền phải lớn hơn hoặc bằng 0")
    private BigDecimal pricePerPack;

    @NotNull(message = "Số năm hút thuốc không được để trống")
    @Min(value = 0, message = "Số năm hút thuốc phải lớn hơn hoặc bằng 0")
    private Integer durationYears;

    @Min(value = 1, message = "Tuổi bắt đầu hút thuốc phải lớn hơn 0")
    private Integer ageStartedSmoking;

    private String brandPreference;

    private String triggers;

    @Min(value = 0, message = "Số lần cai thuốc phải lớn hơn hoặc bằng 0")
    private Integer previousQuitAttempts;

    @Min(value = 0, message = "Thời gian cai thuốc dài nhất phải lớn hơn hoặc bằng 0")
    private Integer longestQuitPeriodDays;

    @Min(value = 1, message = "Mức độ động lực phải từ 1-10")
    private Integer motivationLevel;

    private String healthConcerns;

    private String notes;
}
