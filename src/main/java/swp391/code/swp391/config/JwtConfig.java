package swp391.code.swp391.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Bean
    public SecretKey jwtSecretKey() {
        return new SecretKeySpec(secretKey.getBytes(), "HmacSHA512");
    }

    @Bean //tạo bean JwtDecoder để Spring Security sử dụng trong việc xác thực JWT và có thể truy cập từ các lớp khác
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) { //Inject jwtSecretKey bean bên trên
        return NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }
}
