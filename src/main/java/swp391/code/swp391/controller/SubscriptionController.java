package swp391.code.swp391.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.APIResponse;
import swp391.code.swp391.dto.SubscriptionRequestDTO;
import swp391.code.swp391.dto.SubscriptionResponseDTO;
import swp391.code.swp391.service.SubscriptionServiceImpl;

import java.util.List;

@RestController
@RequestMapping("api/subscription")
public class SubscriptionController {
    private final SubscriptionServiceImpl subscription;

    public SubscriptionController(SubscriptionServiceImpl subscription) {
        this.subscription = subscription;
    }

    @GetMapping()
    public ResponseEntity<APIResponse<List<SubscriptionResponseDTO>>> getAllSubscriptions() {
        List<SubscriptionResponseDTO> dto = subscription.getAllSubscriptions();
        return ResponseEntity.ok(
                APIResponse.<List<SubscriptionResponseDTO>>builder()
                        .success(true)
                        .message("Lấy danh sách gói cước thành công")
                        .data(dto)
                        .build()
        );
    }
    @GetMapping("/user/{userId}")
    public ResponseEntity<APIResponse<SubscriptionResponseDTO>> getUserSub(@PathVariable Long userId) {
        SubscriptionResponseDTO dto = subscription.getCurrentSubscription(userId);
        return ResponseEntity.ok(
                APIResponse.<SubscriptionResponseDTO>builder()
                        .success(true)
                        .message("Lấy danh sách gói cước khả dụng cho người dùng thành công")
                        .data(dto)
                        .build()
        );
    }

    @GetMapping("/{subscriptionType}")
    public ResponseEntity<APIResponse<SubscriptionResponseDTO>> getSubUser(@PathVariable String subscriptionType ) {
        SubscriptionResponseDTO dto = subscription.getUserSameSubscription(subscriptionType);
        return ResponseEntity.ok(
                APIResponse.<SubscriptionResponseDTO>builder()
                        .success(true)
                        .message("Lấy danh sách user cua gói cước thành công")
                        .data(dto)
                        .build()
        );
    }

    @PutMapping("/cancel")
    public ResponseEntity<APIResponse<SubscriptionResponseDTO>> cancelSubscription(@RequestBody Long subscriptionId, Long userId) {
        SubscriptionResponseDTO dto = subscription.cancelSubscription(subscriptionId, userId);
        return ResponseEntity.ok(
                APIResponse.<SubscriptionResponseDTO>builder()
                        .success(true)
                        .message("Hủy gói cước thành công")
                        .data(dto)
                        .build()
        );
    }

    /**
     * API cập nhật gói cước cho người dùng
     */
    @PutMapping("/update")
    public ResponseEntity<APIResponse<SubscriptionResponseDTO>> updateUserSubscription(@RequestBody SubscriptionRequestDTO subscriptionRequest) {
        SubscriptionResponseDTO dto = subscription.updateUserSubscription(subscriptionRequest);
        return ResponseEntity.ok(
                APIResponse.<SubscriptionResponseDTO>builder()
                        .success(true)
                        .message("Cập nhật gói cước thành công")
                        .data(dto)
                        .build()
        );
    }

    @PutMapping("/updateSubscription/{subscriptionId}")
    public ResponseEntity<APIResponse<SubscriptionResponseDTO>> updateSubscription(
            @RequestBody SubscriptionRequestDTO subscriptionRequest
            ,@PathVariable Long subscriptionId) {
        SubscriptionResponseDTO dto = subscription.updateSubscriptionPlan(subscriptionId, subscriptionRequest);
        return ResponseEntity.ok(
                APIResponse.<SubscriptionResponseDTO>builder()
                        .success(true)
                        .message("Cập nhật gói cước thành công")
                        .data(dto)
                        .build()
        );
    }
}


