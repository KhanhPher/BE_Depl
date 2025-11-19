package swp391.code.swp391.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import swp391.code.swp391.dto.PriceFactorRequestDTO;
import swp391.code.swp391.dto.PriceFactorResponseDTO;
import swp391.code.swp391.dto.PriceFactorUpdateDTO;
import swp391.code.swp391.entity.PriceFactor;
import swp391.code.swp391.exception.ResourceNotFoundException;
import swp391.code.swp391.repository.PriceFactorRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PriceFactorServiceImpl implements PriceFactorService {

    private final PriceFactorRepository priceFactorRepository;

    @Override
    public List<PriceFactorResponseDTO> getPriceFactorsByStation(Long stationId) {
        List<PriceFactor> factors = priceFactorRepository.findByStationId(stationId);
        return factors.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    @Override
    public PriceFactorResponseDTO getPriceFactorById(Long id) {
        PriceFactor priceFactor = priceFactorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Price factor not found with id: " + id));
        return toResponseDTO(priceFactor);
    }

    @Override
    public PriceFactorResponseDTO createPriceFactor(PriceFactorRequestDTO requestDTO) {
        // Validate time range
        if (requestDTO.getStartTime().isAfter(requestDTO.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        // Validate factor value
        if (requestDTO.getFactor() <= 0) {
            throw new IllegalArgumentException("Factor must be greater than 0");
        }

        // Check for overlapping time periods
        List<PriceFactor> existingFactors = priceFactorRepository.findByStationId(requestDTO.getStationId());
        for (PriceFactor pf : existingFactors) {
            if (isOverlapping(pf.getStartTime(), pf.getEndTime(),
                    requestDTO.getStartTime(), requestDTO.getEndTime())) {
                throw new IllegalArgumentException("Time period overlaps with existing price factor");
            }
        }

        PriceFactor priceFactor = toEntity(requestDTO);
        PriceFactor saved = priceFactorRepository.save(priceFactor);
        return toResponseDTO(saved);
    }

    @Override
    public PriceFactorResponseDTO updatePriceFactor(Long id, PriceFactorUpdateDTO updateDTO) {
        PriceFactor existing = priceFactorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Price factor not found with id: " + id));

        // Validate required fields
        if (updateDTO.getFactor() == null || updateDTO.getStartTime() == null || updateDTO.getEndTime() == null) {
            throw new IllegalArgumentException("Factor, start time, and end time are required");
        }

        // Validate time range
        if (updateDTO.getStartTime().isAfter(updateDTO.getEndTime())) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        // Validate factor value
        if (updateDTO.getFactor() <= 0) {
            throw new IllegalArgumentException("Factor must be greater than 0");
        }

        // Check for overlapping time periods, excluding itself
        List<PriceFactor> existingFactors = priceFactorRepository.findByStationId(existing.getStationId());
        for (PriceFactor pf : existingFactors) {
            if (!pf.getPriceFactorId().equals(id) &&
                    isOverlapping(pf.getStartTime(), pf.getEndTime(),
                            updateDTO.getStartTime(), updateDTO.getEndTime())) {
                throw new IllegalArgumentException("Time period overlaps with existing price factor");
            }
        }

        // Update fields (stationId không thay đổi)
        existing.setFactor(updateDTO.getFactor());
        existing.setStartTime(updateDTO.getStartTime());
        existing.setEndTime(updateDTO.getEndTime());
        existing.setDescription(updateDTO.getDescription());

        PriceFactor updated = priceFactorRepository.save(existing);
        return toResponseDTO(updated);
    }

    @Override
    public void deletePriceFactor(Long id) {
        if (!priceFactorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Price factor not found with id: " + id);
        }
        priceFactorRepository.deleteById(id);
    }

    private boolean isOverlapping(LocalDateTime start1, LocalDateTime end1,
                                   LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private PriceFactorResponseDTO toResponseDTO(PriceFactor priceFactor) {
        return PriceFactorResponseDTO.builder()
                .priceFactorId(priceFactor.getPriceFactorId())
                .stationId(priceFactor.getStationId())
                .factor(priceFactor.getFactor())
                .startTime(priceFactor.getStartTime())
                .endTime(priceFactor.getEndTime())
                .description(priceFactor.getDescription())
                .build();
    }

    private PriceFactor toEntity(PriceFactorRequestDTO dto) {
        PriceFactor priceFactor = new PriceFactor();
        // Không set priceFactorId vì đây là tạo mới, ID sẽ được tự động generate
        priceFactor.setStationId(dto.getStationId());
        priceFactor.setFactor(dto.getFactor());
        priceFactor.setStartTime(dto.getStartTime());
        priceFactor.setEndTime(dto.getEndTime());
        priceFactor.setDescription(dto.getDescription());
        return priceFactor;
    }
}
