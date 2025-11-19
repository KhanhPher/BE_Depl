package swp391.code.swp391.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.APIResponse;
import swp391.code.swp391.dto.SubscriptionFeatureDTO;
import swp391.code.swp391.dto.SubscriptionFeatureResponseDTO;
import swp391.code.swp391.service.SubscriptionFeatureService;

import java.util.List;

@RestController
@RequestMapping("/api/subscription-features")
@RequiredArgsConstructor
public class SubscriptionFeatureController {

    private final SubscriptionFeatureService featureService;

    /**
     * CREATE - Tạo feature mới
     */
    @PostMapping
    public ResponseEntity<APIResponse<SubscriptionFeatureResponseDTO>> createFeature(
            @Valid @RequestBody SubscriptionFeatureDTO dto) {

        SubscriptionFeatureResponseDTO feature = featureService.createFeature(dto);

        return ResponseEntity.ok(
                APIResponse.<SubscriptionFeatureResponseDTO>builder()
                        .success(true)
                        .message("Tạo feature thành công")
                        .data(feature)
                        .build()
        );
    }

    /**
     * CREATE - Tạo nhiều features cùng lúc
     */
    @PostMapping("/bulk")
    public ResponseEntity<APIResponse<List<SubscriptionFeatureResponseDTO>>> createBulkFeatures(
            @Valid @RequestBody List<SubscriptionFeatureDTO> dtos) {

        List<SubscriptionFeatureResponseDTO> features = featureService.createBulkFeatures(dtos);

        return ResponseEntity.ok(
                APIResponse.<List<SubscriptionFeatureResponseDTO>>builder()
                        .success(true)
                        .message("Tạo " + features.size() + " features thành công")
                        .data(features)
                        .build()
        );
    }

    /**
     * READ - Lấy feature theo ID
     */
    @GetMapping("/{featureId}")
    public ResponseEntity<APIResponse<SubscriptionFeatureResponseDTO>> getFeature(
            @PathVariable Long featureId) {

        SubscriptionFeatureResponseDTO feature = featureService.getFeatureById(featureId);

        return ResponseEntity.ok(
                APIResponse.<SubscriptionFeatureResponseDTO>builder()
                        .success(true)
                        .data(feature)
                        .build()
        );
    }

    /**
     * READ - Lấy tất cả features
     */
    @GetMapping
    public ResponseEntity<APIResponse<List<SubscriptionFeatureResponseDTO>>> getAllFeatures() {

        List<SubscriptionFeatureResponseDTO> features = featureService.getAllFeatures();

        return ResponseEntity.ok(
                APIResponse.<List<SubscriptionFeatureResponseDTO>>builder()
                        .success(true)
                        .message("Tìm thấy " + features.size() + " features")
                        .data(features)
                        .build()
        );
    }

    /**
     * READ - Lấy features của một subscription
     */
    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<APIResponse<List<SubscriptionFeatureResponseDTO>>> getFeaturesBySubscription(
            @PathVariable Long subscriptionId) {

        List<SubscriptionFeatureResponseDTO> features = featureService.getFeaturesBySubscription(subscriptionId);

        return ResponseEntity.ok(
                APIResponse.<List<SubscriptionFeatureResponseDTO>>builder()
                        .success(true)
                        .message("Tìm thấy " + features.size() + " features")
                        .data(features)
                        .build()
        );
    }

    /**
     * READ - Lấy feature theo subscription và key
     */
    @GetMapping("/subscription/{subscriptionId}/key/{featureKey}")
    public ResponseEntity<APIResponse<SubscriptionFeatureResponseDTO>> getFeatureBySubscriptionAndKey(
            @PathVariable Long subscriptionId,
            @PathVariable String featureKey) {

        SubscriptionFeatureResponseDTO feature = featureService.getFeatureBySubscriptionAndKey(subscriptionId, featureKey);

        return ResponseEntity.ok(
                APIResponse.<SubscriptionFeatureResponseDTO>builder()
                        .success(true)
                        .data(feature)
                        .build()
        );
    }

    /**
     * UPDATE - Cập nhật feature đầy đủ
     */
    @PutMapping("/{featureId}")
    public ResponseEntity<APIResponse<SubscriptionFeatureResponseDTO>> updateFeature(
            @PathVariable Long featureId,
            @Valid @RequestBody SubscriptionFeatureDTO dto) {

        SubscriptionFeatureResponseDTO feature = featureService.updateFeature(featureId, dto);

        return ResponseEntity.ok(
                APIResponse.<SubscriptionFeatureResponseDTO>builder()
                        .success(true)
                        .message("Cập nhật feature thành công")
                        .data(feature)
                        .build()
        );
    }

    /**
     * UPDATE - Cập nhật chỉ value
     */
    @PatchMapping("/{featureId}/value")
    public ResponseEntity<APIResponse<SubscriptionFeatureResponseDTO>> updateFeatureValue(
            @PathVariable Long featureId,
            @RequestParam String value) {

        SubscriptionFeatureResponseDTO feature = featureService.updateFeatureValue(featureId, value);

        return ResponseEntity.ok(
                APIResponse.<SubscriptionFeatureResponseDTO>builder()
                        .success(true)
                        .message("Cập nhật giá trị feature thành công")
                        .data(feature)
                        .build()
        );
    }

    /**
     * DELETE - Xóa feature
     */
    @DeleteMapping("/{featureId}")
    public ResponseEntity<APIResponse<Void>> deleteFeature(@PathVariable Long featureId) {

        featureService.deleteFeature(featureId);

        return ResponseEntity.ok(
                APIResponse.<Void>builder()
                        .success(true)
                        .message("Xóa feature thành công")
                        .build()
        );
    }

    /**
     * DELETE - Xóa tất cả features của một subscription
     */
    @DeleteMapping("/subscription/{subscriptionId}")
    public ResponseEntity<APIResponse<Void>> deleteFeaturesBySubscription(@PathVariable Long subscriptionId) {

        featureService.deleteFeaturesBySubscription(subscriptionId);

        return ResponseEntity.ok(
                APIResponse.<Void>builder()
                        .success(true)
                        .message("Xóa tất cả features của subscription thành công")
                        .build()
        );
    }
}