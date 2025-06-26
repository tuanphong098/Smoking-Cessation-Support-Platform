package edu.fpt.smokingcessionnew.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập token JWT của bạn ở đây (không cần thêm prefix 'Bearer')")))
                .info(new Info()
                        .title("Smoking Cessation API")
                        .description("REST API cho ứng dụng cai thuốc lá<br>" +
                                     "<b>Lưu ý quan trọng:</b> Sau khi đăng nhập, sao chép token từ response và " +
                                     "nhấp vào nút 'Authorize' ở đầu trang để nhập token. " +
                                     "Điều này sẽ tự động gửi token với mỗi API request.")
                        .version("1.0")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }
}
