package swp391.code.swp391.service;

import swp391.code.swp391.dto.PriceFactorRequestDTO;
import swp391.code.swp391.dto.PriceFactorResponseDTO;
import swp391.code.swp391.dto.PriceFactorUpdateDTO;
import java.util.List;

public interface PriceFactorService {

    List<PriceFactorResponseDTO> getPriceFactorsByStation(Long stationId);

    PriceFactorResponseDTO getPriceFactorById(Long id);

    PriceFactorResponseDTO createPriceFactor(PriceFactorRequestDTO requestDTO);

    PriceFactorResponseDTO updatePriceFactor(Long id, PriceFactorUpdateDTO updateDTO);

    void deletePriceFactor(Long id);
}
