package swp391.code.swp391.service;

import swp391.code.swp391.dto.RevenueFilterRequestDTO;
import swp391.code.swp391.dto.RevenueResponseDTO;

import java.io.ByteArrayOutputStream;

public interface RevenueService {

    /**
     * Lấy báo cáo doanh thu với filter
     */
    RevenueResponseDTO getRevenueReport(RevenueFilterRequestDTO filter);

    /**
     * Xuất báo cáo doanh thu ra PDF
     */
    ByteArrayOutputStream exportRevenueToPDF(RevenueFilterRequestDTO filter);

    /**
     * Xuất báo cáo doanh thu ra Excel
     */
    ByteArrayOutputStream exportRevenueToExcel(RevenueFilterRequestDTO filter);

    /**
     * Validate filter request
     */
    void validateFilter(RevenueFilterRequestDTO filter);
}