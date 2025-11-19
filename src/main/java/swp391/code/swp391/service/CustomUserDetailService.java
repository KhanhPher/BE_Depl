package swp391.code.swp391.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.code.swp391.entity.CustomUserDetails;
import swp391.code.swp391.entity.User;
import swp391.code.swp391.repository.UserRepository;
import swp391.code.swp391.util.JwtUtil;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException { // Spring Security sử dụng method này để load user
        User user;
        if (jwtUtil.isValidEmail(username)) {
            user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
        } else {
            user = userRepository.findByPhone(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with phone: " + username));
        }
        if (user.getStatus() == User.UserStatus.INACTIVE) {
            throw new DisabledException("User account is inactive");
        }
        if (user.getPassword()==null){
            throw new BadCredentialsException("Account not exist, try again or try another login method");
        }
        return new CustomUserDetails(user, username);
    }

    public User getUserByUserIdentifier(String userIdentifier) {
        User user;
        if (jwtUtil.isValidEmail(userIdentifier)) {
            user = userRepository.findByEmail(userIdentifier)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userIdentifier));
        } else {
            user = userRepository.findByPhone(userIdentifier)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with phone: " + userIdentifier));
        }
        return user;
    }
}
