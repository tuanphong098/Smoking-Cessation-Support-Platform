package edu.fpt.smokingcessionnew.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Pattern(regexp = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", message = "Email không hợp lệ")
    @Schema(description = "Email của người dùng", example = "user@example.com")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @Schema(description = "Mật khẩu có ít nhất 8 ký tự", example = "password123")
    private String password;

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    @Schema(description = "Xác nhận lại mật khẩu", example = "password123")
    private String confirmPassword;

    @NotBlank(message = "Họ và tên không được để trống")
    @Schema(description = "Họ và tên đầy đủ", example = "Nguyễn Văn A")
    private String fullName;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ")
    @Schema(description = "Số điện thoại (10-11 số)", example = "0912345678")
    private String phone;

    @NotNull(message = "Vai trò không được để trống")
    @Schema(description = "Vai trò của người dùng (2=Member, 3=Coach)", example = "2",
            allowableValues = {"2", "3"})
    private Integer role; // 2 = Member, 3 = Coach

    // Các trường khác theo mong muốn
}