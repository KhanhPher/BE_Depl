package swp391.code.swp391.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.APIResponse;
import swp391.code.swp391.dto.PriceFactorRequestDTO;
import swp391.code.swp391.dto.PriceFactorResponseDTO;
import swp391.code.swp391.dto.PriceFactorUpdateDTO;
import swp391.code.swp391.service.PriceFactorService;

import java.util.List;

@RestController
@RequestMapping("/api/price-factors")
@RequiredArgsConstructor
public class PriceFactorController {

    private final PriceFactorService priceFactorService;

    @GetMapping("/station/{stationId}")
    public ResponseEntity<APIResponse<List<PriceFactorResponseDTO>>> getPriceFactorsByStation(@PathVariable Long stationId) {
        List<PriceFactorResponseDTO> factors = priceFactorService.getPriceFactorsByStation(stationId);
        return ResponseEntity.ok(APIResponse.<List<PriceFactorResponseDTO>>builder()
                .success(true)
                .message("Price factors retrieved successfully")
                .data(factors)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<PriceFactorResponseDTO>> getPriceFactorById(@PathVariable Long id) {
        PriceFactorResponseDTO factor = priceFactorService.getPriceFactorById(id);
        return ResponseEntity.ok(APIResponse.<PriceFactorResponseDTO>builder()
                .success(true)
                .message("Price factor retrieved successfully")
                .data(factor)
                .build());
    }

    @PostMapping
    public ResponseEntity<APIResponse<PriceFactorResponseDTO>> createPriceFactor(
            @Valid @RequestBody PriceFactorRequestDTO requestDTO) {
        PriceFactorResponseDTO created = priceFactorService.createPriceFactor(requestDTO);
        return new ResponseEntity<>(APIResponse.<PriceFactorResponseDTO>builder()
                .success(true)
                .message("Price factor created successfully")
                .data(created)
                .build(), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<PriceFactorResponseDTO>> updatePriceFactor(
            @PathVariable Long id,
            @Valid @RequestBody PriceFactorUpdateDTO updateDTO) {
        PriceFactorResponseDTO updated = priceFactorService.updatePriceFactor(id, updateDTO);
        return ResponseEntity.ok(APIResponse.<PriceFactorResponseDTO>builder()
                .success(true)
                .message("Price factor updated successfully")
                .data(updated)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Void>> deletePriceFactor(@PathVariable Long id) {
        priceFactorService.deletePriceFactor(id);
        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .message("Price factor deleted successfully")
                .build());
    }
}