package edu.fpt.smokingcessionnew.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Pattern(regexp = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", message = "Email không hợp lệ")
    @Schema(description = "Email người dùng", example = "user@example.com")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Schema(description = "Mật khẩu đăng nhập", example = "password123")
    private String password;

    @NotNull(message = "Vai trò không được để trống")
    @Schema(description = "Vai trò đăng nhập (2=Member, 3=Coach)", example = "2",
            allowableValues = {"2", "3"})
    private Integer role; // 2 = Member, 3 = Coach
}