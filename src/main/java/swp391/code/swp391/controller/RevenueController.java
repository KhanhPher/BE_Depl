package swp391.code.swp391.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swp391.code.swp391.dto.APIResponse;
import swp391.code.swp391.dto.RevenueFilterRequestDTO;
import swp391.code.swp391.dto.RevenueResponseDTO;
import swp391.code.swp391.entity.Transaction;
import swp391.code.swp391.service.RevenueService;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller cho chức năng theo dõi doanh thu (ADMIN only)
 */
@RestController
@RequestMapping("/api/admin/revenue")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RevenueController {

    private final RevenueService revenueService;

    /**
     * Lấy báo cáo doanh thu với filter
     * GET /api/admin/revenue
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<RevenueResponseDTO>> getRevenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) Transaction.PaymentMethod paymentMethod,
            @RequestParam(required = false) Transaction.Status status,
            @RequestParam(defaultValue = "DAY") String groupBy
    ) {
        try {
            log.info("API: Admin lấy báo cáo doanh thu - từ: {}, đến: {}, trạm: {}",
                    fromDate, toDate, stationId);

            RevenueFilterRequestDTO filter = RevenueFilterRequestDTO.builder()
                    .fromDate(fromDate)
                    .toDate(toDate)
                    .stationId(stationId)
                    .paymentMethod(paymentMethod)
                    .status(status)
                    .groupBy(groupBy)
                    .build();

            RevenueResponseDTO report = revenueService.getRevenueReport(filter);

            return ResponseEntity.ok(
                    APIResponse.<RevenueResponseDTO>builder()
                            .success(true)
                            .message("Lấy báo cáo doanh thu thành công")
                            .data(report)
                            .build()
            );

        } catch (IllegalArgumentException e) {
            log.error("Lỗi validate filter: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.<RevenueResponseDTO>builder()
                            .success(false)
                            .message("Lỗi: " + e.getMessage())
                            .build()
                    );
        } catch (Exception e) {
            log.error("Lỗi khi lấy báo cáo doanh thu: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.<RevenueResponseDTO>builder()
                            .success(false)
                            .message("Lỗi hệ thống: " + e.getMessage())
                            .build()
                    );
        }
    }

    /**
     * Xuất báo cáo doanh thu ra Excel
     * GET /api/admin/revenue/export/excel
     */
    @GetMapping("/export/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> exportRevenueToExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) Transaction.PaymentMethod paymentMethod,
            @RequestParam(required = false) Transaction.Status status,
            @RequestParam(defaultValue = "DAY") String groupBy
    ) {
        try {
            log.info("API: Admin xuất báo cáo doanh thu Excel - từ: {}, đến: {}", fromDate, toDate);

            RevenueFilterRequestDTO filter = RevenueFilterRequestDTO.builder()
                    .fromDate(fromDate)
                    .toDate(toDate)
                    .stationId(stationId)
                    .paymentMethod(paymentMethod)
                    .status(status)
                    .groupBy(groupBy)
                    .build();

            ByteArrayOutputStream excelFile = revenueService.exportRevenueToExcel(filter);

            String filename = "BaoCaoDoanhThu_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                    ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelFile.size());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelFile.toByteArray());

        } catch (IllegalArgumentException e) {
            log.error("Lỗi validate filter: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.builder()
                            .success(false)
                            .message("Lỗi: " + e.getMessage())
                            .build()
                    );
        } catch (Exception e) {
            log.error("Lỗi khi xuất Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.builder()
                            .success(false)
                            .message("Lỗi khi xuất Excel: " + e.getMessage())
                            .build()
                    );
        }
    }

    /**
     * Xuất báo cáo doanh thu ra PDF
     * GET /api/admin/revenue/export/pdf
     */
    @GetMapping("/export/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> exportRevenueToPDF(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) Transaction.PaymentMethod paymentMethod,
            @RequestParam(required = false) Transaction.Status status,
            @RequestParam(defaultValue = "DAY") String groupBy
    ) {
        try {
            log.info("API: Admin xuất báo cáo doanh thu PDF - từ: {}, đến: {}", fromDate, toDate);

            RevenueFilterRequestDTO filter = RevenueFilterRequestDTO.builder()
                    .fromDate(fromDate)
                    .toDate(toDate)
                    .stationId(stationId)
                    .paymentMethod(paymentMethod)
                    .status(status)
                    .groupBy(groupBy)
                    .build();

            ByteArrayOutputStream pdfFile = revenueService.exportRevenueToPDF(filter);

            String filename = "BaoCaoDoanhThu_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                    ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdfFile.size());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfFile.toByteArray());

        } catch (UnsupportedOperationException e) {
            log.warn("PDF export chưa được implement: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body(APIResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build()
                    );
        } catch (IllegalArgumentException e) {
            log.error("Lỗi validate filter: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.builder()
                            .success(false)
                            .message("Lỗi: " + e.getMessage())
                            .build()
                    );
        } catch (Exception e) {
            log.error("Lỗi khi xuất PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.builder()
                            .success(false)
                            .message("Lỗi khi xuất PDF: " + e.getMessage())
                            .build()
                    );
        }
    }
}