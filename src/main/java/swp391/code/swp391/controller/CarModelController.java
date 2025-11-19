package swp391.code.swp391.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.APIResponse;
import swp391.code.swp391.dto.CarModelDTO;
import swp391.code.swp391.service.CarModelServiceImpl;

import java.util.List;

@RestController
@RequestMapping("/api/carModel")
@RequiredArgsConstructor
public class CarModelController {
    private final CarModelServiceImpl carModelServiceImpl;


    @GetMapping
    public ResponseEntity<APIResponse<List<CarModelDTO>>> getCarModels() {

       try {
           List<CarModelDTO> carModelDTO = carModelServiceImpl.findAllCarModels();
           return ResponseEntity.ok(APIResponse.success("Đã lấy danh sách carmodel thành công", carModelDTO));
         } catch (Exception e) {
           return ResponseEntity.status(HttpStatus.NOT_FOUND)
                   .body(APIResponse.error("Lấy danh sách car model thất bại: " + e.getMessage()) );
       }
    }

    @GetMapping("/{car_model_id}")
    public ResponseEntity<APIResponse<CarModelDTO>> getCarModelById(@PathVariable Long car_model_id) {
        try {
            CarModelDTO carModelDTO = carModelServiceImpl.getCarModelById(car_model_id);
            return ResponseEntity.ok(APIResponse.success("Đã lấy car model thành công", carModelDTO));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error("Lấy car model thất bại: " + e.getMessage()) );
        }
    }

    @PostMapping
    public ResponseEntity<APIResponse<CarModelDTO>> createCarModel(@RequestBody CarModelDTO carModelDTO) {
        try {
            CarModelDTO createdCarModel = carModelServiceImpl.createCarModel(carModelDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(APIResponse.success("Đã tạo car model thành công", createdCarModel));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error("Tạo car model thất bại: " + e.getMessage()));
        }
    }

    @PutMapping("/{car_model_id}")
    public ResponseEntity<APIResponse<CarModelDTO>> updateCarModel(@PathVariable Long car_model_id, @RequestBody CarModelDTO carModelDTO) {
        try {
            CarModelDTO updatedCarModel = carModelServiceImpl.updateCarModel(car_model_id, carModelDTO);
            return ResponseEntity.ok(APIResponse.success("Đã cập nhật car model thành công", updatedCarModel));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error("Cập nhật car model thất bại: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{car_model_id}")
    public ResponseEntity<APIResponse<CarModelDTO>> deleteCarModel(@PathVariable Long car_model_id) {
        try {
            carModelServiceImpl.deleteCarModel(car_model_id);
            return ResponseEntity.ok(APIResponse.success("Đã xóa car model thành công", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error("Xóa car model thất bại: " + e.getMessage()));
        }
    }

}