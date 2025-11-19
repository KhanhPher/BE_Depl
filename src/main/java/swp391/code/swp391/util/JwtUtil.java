package swp391.code.swp391.util;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import swp391.code.swp391.entity.CustomUserDetails;
import swp391.code.swp391.entity.User;
import swp391.code.swp391.repository.UserRepository;

import java.text.ParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;

    @Value("${app.jwt.secret}")
    private String secretKey;

    public String generateAccessToken(CustomUserDetails user){
        Date issueTime = new Date();
        Date expiredTime = Date.from(issueTime.toInstant().plus(1, ChronoUnit.DAYS));
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512); //HS512: thuật toán băm, mã hóa đối xứng

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder() //tạo các thông tin trong payload
                .subject(user.getUsername())
                .claim("roles", user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList()) // Chuyển đổi GrantedAuthority thành List<String>
                .claim("status", user.getUser().getStatus().name()) // Thêm status
                .issueTime(issueTime) //thời gian tạo token
                .expirationTime(expiredTime) //thời gian hết hạn token
                .build();
        Payload payload = new Payload(claimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header,payload);
        try {
            jwsObject.sign(new MACSigner(secretKey)); //MacSigner: mã hóa đối xứng
            return jwsObject.serialize(); //trả về chuỗi JWT, gồm header.payload.signature
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    public String generateRefreshToken(CustomUserDetails user){
        Date issueTime = new Date();
        Date expiredTime = Date.from(issueTime.toInstant().plus(30, java.time.temporal.ChronoUnit.DAYS));

        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder() //tạo các thông tin trong payload
                .subject(user.getUsername())
                .claim("roles", user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList()) // Chuyển đổi GrantedAuthority thành List<String>
                .claim("status", user.getUser().getStatus().name()) // Thêm status
                .issueTime(issueTime) //thời gian tạo token
                .expirationTime(expiredTime) //thời gian hết hạn token
                .build();
        Payload payload = new Payload(claimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header,payload);
        try {
            jwsObject.sign(new MACSigner(secretKey)); //MacSigner: mã hóa đối xứng
            return jwsObject.serialize(); //trả về chuỗi JWT, gồm header.payload.signature
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    // Lấy token thủ công qua jwtDecoder Bean
    public String extractUsername(String token)throws  ParseException, JOSEException {
        return jwtDecoder.decode(token).getSubject();
    }

    // Lấy role thủ công qua jwtDecoder Bean
    public List<String> extractRole(String token)throws  ParseException, JOSEException {
        return jwtDecoder.decode(token).getClaimAsStringList("roles");
    }

    // Dùng SecurityContextHolder lấy username
    public String getUsernameFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getSubject();
        }
        throw new RuntimeException("No JWT token found in context");
    }

    // Dùng SecurityContextHolder lấy role
    public List<String> getRoleFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getClaimAsStringList("roles");
        }
        throw new RuntimeException("No JWT token found in context");
    }

    //===================================================================================
    // Fetches user by either email or phone
    //For purpose: fetch User in token payload
    public User getUserByTokenDecode(String token) throws ParseException, JOSEException {
        String identifier = extractUsername(token);
        Optional<User> userOptional;
        if (isValidEmail(identifier)) {
            userOptional = userRepository.findByEmail(identifier);
        } else if (isValidVietnamPhone(identifier)) {
            userOptional = userRepository.findByPhone(identifier);
        } else {
            throw new IllegalArgumentException("Invalid email or phone number");
        }
        return userOptional.orElse(null);
    }

    public Long getUserIdByTokenDecode(String token) throws ParseException, JOSEException {
        User user = getUserByTokenDecode(token);
        if (user != null) {
            return user.getUserId();
        }
        throw new IllegalArgumentException("User not found for the provided token");
    }

    public User getUserByTokenThroughSecurityContext() throws ParseException, JOSEException {
        String identifier = getUsernameFromContext();
        Optional<User> userOptional;
        if (isValidEmail(identifier)) {
            userOptional = userRepository.findByEmail(identifier);
        } else if (isValidVietnamPhone(identifier)) {
            userOptional = userRepository.findByPhone(identifier);
        } else {
            throw new IllegalArgumentException("Invalid email or phone number");
        }
        return userOptional.orElse(null);
    }

    public String getTokenFromRequestHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return getTokenFromHeader(header);
    }
    public String getTokenFromHeader(String header) {
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header");
    }

    //-------------------Validation utils-------------------
    /**
     * Validates email format using regex pattern
     */
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    public boolean isValidEmail(String email) {
        return email != null && Pattern.matches(EMAIL_REGEX, email);
    }

    /**
     * Validates Vietnamese phone number format using regex pattern
     */
    private static final String VIETNAM_PHONE_REGEX = "^(0|\\+84)([35789])[0-9]{8}$";
    public boolean isValidVietnamPhone(String phone) {
        return phone != null && Pattern.matches(VIETNAM_PHONE_REGEX, phone);
    }
}

