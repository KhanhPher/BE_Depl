//package swp391.code.swp391.websocket;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Service;
//import swp391.code.swp391.dto.SessionProgressDTO;
//import swp391.code.swp391.entity.User;
//
//@Service
//@RequiredArgsConstructor
//public class SessionWebSocketService {
//
//    private final SimpMessagingTemplate simpMessagingTemplate;
//    public void sendSessionProgressToUser(User user, SessionProgressDTO dto) {
//        if (user == null) return;
//        String username = user.getEmail() != null ? user.getEmail() : user.getPhone();
//        if (username == null) return;
//
//        // send to user's personal queue /user/{username}/queue/session-progress
//        simpMessagingTemplate.convertAndSendToUser(username, "/queue/session-progress", dto);
//    }
//}
//
