package edu.fpt.smokingcessionnew.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private Integer userId;
    private String email;
    private Integer role;
    // Có thể thêm thông tin khác theo nhu cầu
}