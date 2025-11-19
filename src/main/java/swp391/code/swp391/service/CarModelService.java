package swp391.code.swp391.service;

import swp391.code.swp391.dto.CarModelDTO;
import java.util.List;

public interface CarModelService {

    /**
     * Tạo một CarModel mới từ CarModelDTO.
     *
     * @param carModelDTO DTO chứa thông tin CarModel
     * @return CarModelDTO của CarModel vừa tạo
     * @throws RuntimeException nếu CarModel đã tồn tại (dựa trên brand và model)
     */
    CarModelDTO createCarModel(CarModelDTO carModelDTO);

    /**
     * Cập nhật thông tin CarModel theo ID.
     *
     * @param carModelId ID của CarModel cần cập nhật
     * @param carModelDTO DTO chứa thông tin cập nhật
     * @return CarModelDTO của CarModel đã cập nhật
     * @throws RuntimeException nếu CarModel không tồn tại
     */
    CarModelDTO updateCarModel(Long carModelId, CarModelDTO carModelDTO);

    /**
     * Xóa CarModel theo ID.
     *
     * @param carModelId ID của CarModel cần xóa
     * @throws RuntimeException nếu CarModel không tồn tại
     */
    void deleteCarModel(Long carModelId);

    /**
     * Lấy thông tin CarModel theo ID.
     *
     * @param carModelId ID của CarModel
     * @return CarModelDTO của CarModel
     * @throws RuntimeException nếu CarModel không tồn tại
     */
    CarModelDTO getCarModelById(Long carModelId);

    /**
     * Lấy tất cả CarModel.
     *
     * @return Danh sách CarModelDTO
     */
    List<CarModelDTO> findAllCarModels();

    /**
     * Tìm CarModel theo brand (không phân biệt hoa thường).
     *
     * @param brand Tên hãng xe (hoặc một phần của tên)
     * @return Danh sách CarModelDTO khớp với brand
     */
    List<CarModelDTO> findCarModelsByBrand(String brand);

    /**
     * Tìm CarModel theo model (không phân biệt hoa thường).
     *
     * @param model Tên model xe (hoặc một phần của tên)
     * @return Danh sách CarModelDTO khớp với model
     */
    List<CarModelDTO> findCarModelsByModel(String model);

    /**
     * Tìm CarModel theo năm sản xuất.
     *
     * @param productYear Năm sản xuất
     * @return Danh sách CarModelDTO khớp với năm sản xuất
     */
    List<CarModelDTO> findCarModelsByProductYear(int productYear);

    /**
     * Tìm CarModel theo ConnectorType.
     *
     * @param connectorTypeId ID của ConnectorType
     * @return Danh sách CarModelDTO có liên kết với ConnectorType
     */
    List<CarModelDTO> findCarModelsByConnectorType(Long connectorTypeId);

    /**
     * Tìm một CarModel theo Id.
     * @param carmodelId
     * @return CarModelDTO của CarModel vừa tạo
     * @throws RuntimeException nếu CarModel đã tồn tại (dựa trên brand và model)
     */

    CarModelDTO findByCarmodelId(Long carmodelId);
}