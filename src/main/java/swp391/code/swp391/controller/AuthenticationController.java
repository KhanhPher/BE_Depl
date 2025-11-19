package swp391.code.swp391.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.APIResponse;
import swp391.code.swp391.dto.LoginRequestDTO;
import swp391.code.swp391.dto.LoginResponseDTO;
import swp391.code.swp391.dto.RegisterRequestDTO;
import swp391.code.swp391.service.AuthenticationService;
import swp391.code.swp391.util.JwtUtil;
import swp391.code.swp391.service.UserServiceImpl;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final JwtUtil jwtUtil;
    private final UserServiceImpl userServiceImpl;

    @PostMapping("/login")
    ResponseEntity<APIResponse<LoginResponseDTO>> login(@RequestBody LoginRequestDTO loginRequestDTO) {
        try{
            LoginResponseDTO responseDTO = authenticationService.login(loginRequestDTO);
            return ResponseEntity.ok(APIResponse.success("Login successful", responseDTO));
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(APIResponse.error("Login failed: " + e.getMessage()));
        }
    }


    @PostMapping("/register")
    public ResponseEntity<APIResponse<Long>> register(@Valid @RequestBody RegisterRequestDTO registerDTO) {
        try {
            Long userId = userServiceImpl.registerUser(registerDTO);
            if (userId == -1L) {
                return ResponseEntity.badRequest().body(APIResponse.success( "Registration failed", userId));
            }
            return ResponseEntity.ok(APIResponse.success("Registration successful", userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new APIResponse<>(false, e.getMessage(), -1L));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(false, e.getMessage(), -1L));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<APIResponse<String>> logout(HttpServletRequest request) {
        try {
            String token = jwtUtil.getTokenFromRequestHeader(request);
            if (token == null) {
                return ResponseEntity.badRequest().body(APIResponse.error("No token provided"));
            }
            authenticationService.logout(token);
            return ResponseEntity.ok(APIResponse.success("Logout successful", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(APIResponse.error("Logout failed: " + e.getMessage()));
        }
    }

    @GetMapping("/social/login") //?loginType=google
    public ResponseEntity<APIResponse<String>> socialLogin(@RequestParam String loginType) {
        loginType = loginType.trim().toLowerCase();
//        request.getRequestURI();
        String url = authenticationService.getSocialLoginUrl(loginType);
        return ResponseEntity.ok(APIResponse.success("Redirect to social platform", url));
    }

    @GetMapping("/social/callback")
    public ResponseEntity<APIResponse<LoginResponseDTO>> socialCallback(@RequestParam("code") String code,
                                                                        @RequestParam("state") String loginType) {
        try {
            System.out.println(code);
            //String requestUrl = request.getRequestURL().toString();
            LoginResponseDTO responseDTO = authenticationService.generateTokenForSocialUser(code, loginType);
            return ResponseEntity.ok(APIResponse.success("Social login successful", responseDTO)); //
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(APIResponse.error("Login failed: " + e.getMessage()));
        }
    }



    // Lấy userId của user hiện tại từ token
    @PostMapping("/me")
    public ResponseEntity<APIResponse<Long> > getCurrentUserId(HttpServletRequest request) {
        try {
            String token = jwtUtil.getTokenFromRequestHeader(request);
            if (token == null) {
                return ResponseEntity.badRequest().body(APIResponse.error("No token provided"));
            }
            Long userId = jwtUtil.getUserByTokenDecode(token).getUserId();
            return ResponseEntity.ok(APIResponse.success("User ID retrieved successfully", userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(APIResponse.error("Failed to retrieve user ID: " + e.getMessage()));
        }
    }
}
