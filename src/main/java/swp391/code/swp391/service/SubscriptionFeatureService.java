
package swp391.code.swp391.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swp391.code.swp391.dto.SubscriptionFeatureDTO;
import swp391.code.swp391.dto.SubscriptionFeatureResponseDTO;
import swp391.code.swp391.entity.SubscriptionFeature;
import swp391.code.swp391.entity.Subscription;
import swp391.code.swp391.exception.ApiRequestException;
import swp391.code.swp391.repository.SubscriptionFeatureRepository;
import swp391.code.swp391.repository.SubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionFeatureService {

    private final SubscriptionFeatureRepository featureRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * CREATE - Tạo feature mới
     */
    @Transactional
    public SubscriptionFeatureResponseDTO createFeature(SubscriptionFeatureDTO dto) {

        // Validate subscription tồn tại
        Subscription subscription = subscriptionRepository.findById(dto.getSubscriptionId())
                .orElseThrow(() -> new ApiRequestException("Không tìm thấy gói subscription"));

        // Kiểm tra feature key đã tồn tại cho subscription này chưa
        if (featureRepository.existsBySubscriptionAndFeatureKey(subscription, dto.getFeatureKey())) {
            throw new ApiRequestException(
                    "Feature '" + dto.getFeatureKey() + "' đã tồn tại cho gói này"
            );
        }

        // Tạo mới
        SubscriptionFeature feature = new SubscriptionFeature();
        feature.setSubscription(subscription);
        feature.setFeatureKey(dto.getFeatureKey());
        feature.setFeatureValue(dto.getFeatureValue());
        feature.setFeatureType(dto.getFeatureType());
        feature.setDisplayName(dto.getDisplayName());
        feature.setDescription(dto.getDescription());
        feature.setCreatedAt(LocalDateTime.now());

        feature = featureRepository.save(feature);

        return convertToDTO(feature);
    }

    /**
     * READ - Lấy feature theo ID
     */
    @Transactional(readOnly = true)
    public SubscriptionFeatureResponseDTO getFeatureById(Long featureId) {
        SubscriptionFeature feature = featureRepository.findById(featureId)
                .orElseThrow(() -> new ApiRequestException("Không tìm thấy feature"));

        return convertToDTO(feature);
    }

    /**
     * READ - Lấy tất cả features của một subscription
     */
    @Transactional(readOnly = true)
    public List<SubscriptionFeatureResponseDTO> getFeaturesBySubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ApiRequestException("Không tìm thấy gói subscription"));

        return featureRepository.findBySubscription(subscription).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * READ - Lấy tất cả features
     */
    @Transactional(readOnly = true)
    public List<SubscriptionFeatureResponseDTO> getAllFeatures() {
        return featureRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * READ - Lấy feature theo subscription và key
     */
    @Transactional(readOnly = true)
    public SubscriptionFeatureResponseDTO getFeatureBySubscriptionAndKey(Long subscriptionId, String featureKey) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ApiRequestException("Không tìm thấy gói subscription"));

        SubscriptionFeature feature = featureRepository.findBySubscriptionAndFeatureKey(subscription, featureKey)
                .orElseThrow(() -> new ApiRequestException(
                        "Không tìm thấy feature '" + featureKey + "' cho gói này"
                ));

        return convertToDTO(feature);
    }

    /**
     * UPDATE - Cập nhật feature
     */
    @Transactional
    public SubscriptionFeatureResponseDTO updateFeature(Long featureId, SubscriptionFeatureDTO dto) {

        SubscriptionFeature feature = featureRepository.findById(featureId)
                .orElseThrow(() -> new ApiRequestException("Không tìm thấy feature"));

        // Nếu đổi subscription, validate subscription mới
        if (!feature.getSubscription().getSubscriptionId().equals(dto.getSubscriptionId())) {
            Subscription newSubscription = subscriptionRepository.findById(dto.getSubscriptionId())
                    .orElseThrow(() -> new ApiRequestException("Không tìm thấy gói subscription"));
            feature.setSubscription(newSubscription);
        }

        // Update các field
        feature.setFeatureKey(dto.getFeatureKey());
        feature.setFeatureValue(dto.getFeatureValue());
        feature.setFeatureType(dto.getFeatureType());
        feature.setDisplayName(dto.getDisplayName());
        feature.setDescription(dto.getDescription());

        feature = featureRepository.save(feature);

        return convertToDTO(feature);
    }

    /**
     * UPDATE - Cập nhật chỉ giá trị (value) của feature
     */
    @Transactional
    public SubscriptionFeatureResponseDTO updateFeatureValue(Long featureId, String newValue) {

        SubscriptionFeature feature = featureRepository.findById(featureId)
                .orElseThrow(() -> new ApiRequestException("Không tìm thấy feature"));

        feature.setFeatureValue(newValue);
        feature = featureRepository.save(feature);

        return convertToDTO(feature);
    }

    /**
     * DELETE - Xóa feature
     */
    @Transactional
    public void deleteFeature(Long featureId) {

        if (!featureRepository.existsById(featureId)) {
            throw new ApiRequestException("Không tìm thấy feature");
        }

        featureRepository.deleteById(featureId);
    }

    /**
     * DELETE - Xóa tất cả features của một subscription
     */
    @Transactional
    public void deleteFeaturesBySubscription(Long subscriptionId) {

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ApiRequestException("Không tìm thấy gói subscription"));

        featureRepository.deleteBySubscription(subscription);
    }

    /**
     * BULK CREATE - Tạo nhiều features cùng lúc
     */
    @Transactional
    public List<SubscriptionFeatureResponseDTO> createBulkFeatures(List<SubscriptionFeatureDTO> dtos) {

        List<SubscriptionFeature> features = dtos.stream()
                .map(dto -> {
                    Subscription subscription = subscriptionRepository.findById(dto.getSubscriptionId())
                            .orElseThrow(() -> new ApiRequestException("Không tìm thấy gói subscription"));

                    SubscriptionFeature feature = new SubscriptionFeature();
                    feature.setSubscription(subscription);
                    feature.setFeatureKey(dto.getFeatureKey());
                    feature.setFeatureValue(dto.getFeatureValue());
                    feature.setFeatureType(dto.getFeatureType());
                    feature.setDisplayName(dto.getDisplayName());
                    feature.setDescription(dto.getDescription());
                    feature.setCreatedAt(LocalDateTime.now());

                    return feature;
                })
                .collect(Collectors.toList());

        features = featureRepository.saveAll(features);

        return features.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convert entity to DTO
     */
    private SubscriptionFeatureResponseDTO convertToDTO(SubscriptionFeature feature) {
        return SubscriptionFeatureResponseDTO.builder()
                .featureId(feature.getFeatureId())
                .subscriptionId(feature.getSubscription().getSubscriptionId())
                .subscriptionName(feature.getSubscription().getSubscriptionName()) // Assuming Subscription has getName()
                .featureKey(feature.getFeatureKey())
                .featureValue(feature.getFeatureValue())
                .featureType(feature.getFeatureType().name())
                .displayName(feature.getDisplayName())
                .description(feature.getDescription())
                .createdAt(feature.getCreatedAt())
                .build();
    }
}
