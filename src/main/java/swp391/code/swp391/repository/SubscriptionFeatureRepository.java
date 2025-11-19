package swp391.code.swp391.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.code.swp391.entity.SubscriptionFeature;
import swp391.code.swp391.entity.Subscription;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionFeatureRepository extends JpaRepository<SubscriptionFeature, Long> {

    List<SubscriptionFeature> findBySubscription(Subscription subscription);

    Optional<SubscriptionFeature> findBySubscriptionAndFeatureKey(Subscription subscription, String featureKey);

    boolean existsBySubscriptionAndFeatureKey(Subscription subscription, String featureKey);

    void deleteBySubscription(Subscription subscription);

    List<SubscriptionFeature> findByFeatureKey(String featureKey);
}