package swp391.code.swp391.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import swp391.code.swp391.dto.LoginRequestDTO;
import swp391.code.swp391.dto.LoginResponseDTO;
import swp391.code.swp391.entity.CustomUserDetails;
import swp391.code.swp391.entity.User;
import swp391.code.swp391.repository.SubscriptionRepository;
import swp391.code.swp391.util.JwtUtil;

import java.io.IOException;
import java.util.Map;

import static swp391.code.swp391.entity.Subscription.Type.BASIC;

@Service
@RequiredArgsConstructor // This annotation generates a constructor with required arguments (final fields)
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserServiceImpl userServiceImpl;
    private final JwtBlacklistService jwtBlacklistService;
    private final SubscriptionRepository subscriptionRepository;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    @Value("${spring.security.oauth2.client.provider.google.user-info-uri}")
    private String googleUserInfoUri;

    @Value("${spring.security.oauth2.client.registration.facebook.client-id}")
    private String facebookClientId;

    @Value("${spring.security.oauth2.client.registration.facebook.client-secret}")
    private String facebookClientSecret;

    @Value("${spring.security.oauth2.client.registration.facebook.redirect-uri}")
    private String facebookRedirectUri;

    @Value("${spring.security.oauth2.client.provider.facebook.user-info-uri}")
    private String facebookUserInfoUri;

    @Value("${spring.security.oauth2.client.provider.facebook.token-uri}")
    private String facebookTokenUri;

    @Value("${spring.security.oauth2.client.registration.facebook.scope}")
    private String facebookScope;
    public LoginResponseDTO login(LoginRequestDTO request) {
        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());

            Authentication authenticate =  authenticationManager.authenticate(authenticationToken); // Xác thực người dùng
            CustomUserDetails customUserDetails = (CustomUserDetails) authenticate.getPrincipal();  // Hoan thanh xac thuc, lấy thông tin user

            // Tạo token
            String accessToken = jwtUtil.generateAccessToken(customUserDetails); //Payload chứa thông tin user, issueTime, expiredTime
            String refreshToken = jwtUtil.generateRefreshToken(customUserDetails); //Payload chứa thông tin user, issueTime, expiredTime

            // Nếu thành công → trả token
            return LoginResponseDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();

        } catch (BadCredentialsException e) {
            throw new RuntimeException("AuthenticationService_login():Invalid username or password"); // Xử lý lỗi xác thực
        } catch (Exception e) {
            throw new RuntimeException("AuthenticationService_login(): Authentication failed: " + e.getMessage()); // Xử lý các lỗi khác
        }
    }

    public void logout(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("AuthenticationService_logout(): Invalid token");
        }
        jwtBlacklistService.blacklistToken(token);
    }

    public String getSocialLoginUrl(String loginType) {
        // Trả về URL đăng nhập xã hội dựa trên loại đăng nhập
        switch (loginType.toLowerCase()) {
            case "google":
                return "https://accounts.google.com/o/oauth2/v2/auth?" +
                        "client_id=" + googleClientId +
                        "&redirect_uri=" + googleRedirectUri +
                        "&response_type=code" +
                        "&scope=openid%20email%20profile" +
                        "&state=" + loginType;
            case "facebook":
                return "https://www.facebook.com/v23.0/dialog/oauth?" +
                        "client_id=" + facebookClientId +
                        "&redirect_uri=" + facebookRedirectUri +
                        "&response_type=code" +
                        "&scope=" + facebookScope
                        + "&state=" + loginType;
            default:
                throw new IllegalArgumentException("AuthenticationService_social-login():Unsupported login type: " + loginType);
        }
    }


    public Map<String, Object> authenticateAndFetchProfile(String code, String loginType) throws IOException {
        // Xác thực với nhà cung cấp xã hội và lấy thông tin hồ sơ người dùng
        RestTemplate restTemplate = new RestTemplate(); // Tạo RestTemplate để thực hiện các yêu cầu HTTP

        String accessToken;

        switch (loginType.toLowerCase()) {
            case "google":
                // Bước 1: Đổi mã code lấy access token
                accessToken = new GoogleAuthorizationCodeTokenRequest(
                        new NetHttpTransport(), new GsonFactory(),
                        googleClientId,
                        googleClientSecret,
                        code,
                        googleRedirectUri
                ).execute().getAccessToken(); // Lấy access token từ phản hồi

                // Cấu hình RestTemplate để thêm access token vào tiêu đề Authorization
                restTemplate.getInterceptors().add((request, body, execution) -> {
                    request.getHeaders().add("Authorization", "Bearer " + accessToken);
                    return execution.execute(request, body);
                });

                // Make a Get request to fetch user info
                return new ObjectMapper().readValue(
                        restTemplate.getForEntity(googleUserInfoUri, String.class).getBody(),
                        new TypeReference<>() {}); // Chuyển đổi JSON response thành Map

            case "facebook":
                String urlGetAccessToken = UriComponentsBuilder
                        .fromUriString(facebookTokenUri)
                        .queryParam("client_id",facebookClientId)
                        .queryParam("redirect_uri",facebookRedirectUri)
                        .queryParam("client_secret",facebookClientSecret)
                        .queryParam("code",code)
                        .toUriString();

                ResponseEntity<String> response = restTemplate.getForEntity(urlGetAccessToken, String.class);
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(response.getBody());
                accessToken = node.get("access_token").asText();

                String userInfoUri = facebookUserInfoUri + "&access_token=" +accessToken;
                return mapper.readValue(
                        restTemplate.getForEntity(userInfoUri, String.class).getBody(),
                        new TypeReference<>() {});
            default:
                throw new IllegalArgumentException("Unsupported login type: " + loginType);
        }
    }

    public CustomUserDetails processSocialLogin(String code, String loginType){
        User user;
        CustomUserDetails customUserDetails;
        try {
            Map<String, Object> userInfo = authenticateAndFetchProfile(code, loginType);
            if (userInfo == null || userInfo.isEmpty()) {
                throw new RuntimeException("Failed to fetch user info from " + loginType);
            }
            // Process userInfo to register or login the user in your system
            String accountId;
            String email; //null neu login FB tra ve phone
            String name;
            String avatarUrl;
            String phone = null; //null neu login GG tra ve email

            if (loginType.trim().equalsIgnoreCase("google")) {
                accountId = userInfo.get("sub").toString();
                email = (String) userInfo.get("email");
                name = (String) userInfo.get("name");
                avatarUrl = (String) userInfo.get("picture");
            } else if (loginType.trim().equalsIgnoreCase("facebook")) {
                accountId = userInfo.get("id").toString();
                email = (String) userInfo.get("email");
                phone = (String) userInfo.get("phone"); //FB có thể trả về phone thay vì email
                name = (String) userInfo.get("name");
                avatarUrl = "https://graph.facebook.com/" + accountId + "/picture";
            } else {
                throw new IllegalArgumentException("Unsupported login type: " + loginType);
            }

            user = User.builder()
                    .email(email)
                    .fullName(name)
                    .phone(phone)
                    .password(null)
                    .avatar(avatarUrl)
                    .build();
            if (loginType.trim().equalsIgnoreCase("facebook")) {
                user.setFacebookId(accountId);
            } else if (loginType.trim().equalsIgnoreCase("google")) {
                user.setGoogleId(accountId);
            }
            // Lưu user vào DB nếu chưa tồn tại
            //Neu loginGG ma email da ton tai thi lien ket tai khoan GG voi user do
            //Neu loginFB ma username da ton tai thi lien ket tai khoan FB voi user
            Long userId = saveOrLinkSocialAccount(user, loginType);
            user = userServiceImpl.getUserById(userId);
            System.out.println("user after save: " + user);

            customUserDetails = new CustomUserDetails();
            customUserDetails.setUser(user);
            customUserDetails.setUserIdentifier(email != null ? email : phone); // Sử dụng email nếu có, nếu không thì dùng phone
        }
        catch (Exception e){
            throw new RuntimeException("Social login failed: " + e.getMessage());
        }
        return customUserDetails;
    }

    @Transactional
    public LoginResponseDTO generateTokenForSocialUser(String code, String loginType){
        CustomUserDetails customUserDetails = processSocialLogin(code, loginType);
        return LoginResponseDTO.builder()
                .accessToken(jwtUtil.generateAccessToken(customUserDetails))
                .refreshToken(jwtUtil.generateRefreshToken(customUserDetails))
                .build();
    }

    // Lưu user mới hoặc liên kết tài khoản xã hội với user hiện có
    private Long saveOrLinkSocialAccount(User user, String loginType) {
        User existingUser = null;

        // Ưu tiên tìm theo email
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            existingUser = userServiceImpl.getUserByMail(user.getEmail());
        }

        // Nếu không có email hoặc không tìm thấy, fallback sang số điện thoại
        //Thực tế không làm được vì không trả về phone
        if (existingUser == null && user.getPhone() != null && !user.getPhone().isEmpty()) {
            existingUser = userServiceImpl.getUserByPhone(user.getPhone());
        }

        if (existingUser != null) {
            // Link social ID vào user cũ
            if ("google".equalsIgnoreCase(loginType)) {
                existingUser.setGoogleId(user.getGoogleId());
                existingUser.setAvatar(user.getAvatar());
            } else if ("facebook".equalsIgnoreCase(loginType)) {
                existingUser.setFacebookId(user.getFacebookId());
                existingUser.setAvatar(user.getAvatar());
            }
            return userServiceImpl.addUser(existingUser); //khong nen add ma nen update
        }

        // Nếu không có user nào trùng → tạo mới
        user.setStatus(User.UserStatus.ACTIVE);
        user.setRole(User.UserRole.DRIVER);
        user.setSubscription(subscriptionRepository.findByType(BASIC));
        return userServiceImpl.addUser(user);
    }
}
