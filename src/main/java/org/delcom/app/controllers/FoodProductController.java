package org.delcom.app.controllers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.delcom.app.configs.ApiResponse;
import org.delcom.app.configs.AuthContext;
import org.delcom.app.dto.FoodProductForm;
import org.delcom.app.dto.ProductImageForm;
import org.delcom.app.entities.FoodProduct;
import org.delcom.app.entities.User;
import org.delcom.app.services.FoodQualityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/food-products")
public class FoodProductController {

    private final FoodQualityService foodQualityService;

    @Autowired
    protected AuthContext authContext;

    public FoodProductController(FoodQualityService foodQualityService) {
        this.foodQualityService = foodQualityService;
    }

    // ==================================================================================
    // 1. CREATE - Mendaftarkan Produk Pangan Baru untuk Inspeksi
    // ==================================================================================
    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ApiResponse<Map<String, UUID>>> createProduct(@ModelAttribute FoodProductForm form) {

        // Validasi Manual Field Wajib
        if (form.getProductName() == null || form.getProductName().isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("fail", "Nama produk tidak valid", null));
        } else if (form.getBatchCode() == null || form.getBatchCode().isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("fail", "Kode batch tidak valid", null));
        } else if (form.getInspectionStatus() == null || form.getInspectionStatus().isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("fail", "Status inspeksi tidak valid", null));
        } else if (form.getImageFile() == null || form.getImageFile().isEmpty()) {
            // Wajib ada foto sampel produk saat create
            return ResponseEntity.badRequest().body(new ApiResponse<>("fail", "Foto sampel produk wajib diupload", null));
        }

        // Cek Autentikasi
        if (!authContext.isAuthenticated()) {
            return ResponseEntity.status(403).body(new ApiResponse<>("fail", "User tidak terautentikasi", null));
        }
        User authUser = authContext.getAuthUser();

        // Panggil Service
        FoodProduct newProduct = foodQualityService.createProduct(authUser.getId(), form);

        return ResponseEntity.ok(new ApiResponse<>(
                "success",
                "Berhasil mendaftarkan produk pangan",
                Map.of("id", newProduct.getId())));
    }

    // ==================================================================================
    // 2. READ - Mendapatkan Daftar Produk (Support Search by Name/Batch)
    // ==================================================================================
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, List<FoodProduct>>>> getAllProducts(
            @RequestParam(required = false) String search) {
        
        if (!authContext.isAuthenticated()) {
            return ResponseEntity.status(403).body(new ApiResponse<>("fail", "User tidak terautentikasi", null));
        }
        User authUser = authContext.getAuthUser();

        List<FoodProduct> products = foodQualityService.getAllProducts(authUser.getId(), search);
        
        return ResponseEntity.ok(new ApiResponse<>(
                "success",
                "Berhasil mengambil data produk",
                Map.of("food_products", products)));
    }

    // ==================================================================================
    // 3. READ DETAIL - Mendapatkan 1 Produk berdasarkan ID
    // ==================================================================================
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, FoodProduct>>> getProductById(@PathVariable UUID id) {
        
        if (!authContext.isAuthenticated()) {
            return ResponseEntity.status(403).body(new ApiResponse<>("fail", "User tidak terautentikasi", null));
        }
        User authUser = authContext.getAuthUser();

        FoodProduct product = foodQualityService.getProductById(authUser.getId(), id);
        if (product == null) {
            return ResponseEntity.status(404).body(new ApiResponse<>("fail", "Produk tidak ditemukan", null));
        }

        return ResponseEntity.ok(new ApiResponse<>(
                "success",
                "Berhasil mengambil detail produk",
                Map.of("food_product", product)));
    }

    // ==================================================================================
    // 4. GET BATCHES - Mendapatkan List Kode Batch (Untuk Dropdown Filter)
    // ==================================================================================
    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> getBatches() {
        if (!authContext.isAuthenticated()) {
            return ResponseEntity.status(403).body(new ApiResponse<>("fail", "User tidak terautentikasi", null));
        }
        User authUser = authContext.getAuthUser();

        List<String> batches = foodQualityService.getAllBatchCodes(authUser.getId());
        return ResponseEntity.ok(new ApiResponse<>(
                "success",
                "Berhasil mengambil data batch",
                Map.of("batches", batches)));
    }

    // ==================================================================================
    // 5. UPDATE - Mengubah Data Inspeksi (Text & Gambar Opsional)
    // ==================================================================================
    @PutMapping(value = "/{id}", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ApiResponse<FoodProduct>> updateProduct(
            @PathVariable UUID id, 
            @ModelAttribute FoodProductForm form) {

        // Validasi input text
        if (form.getProductName() == null || form.getProductName().isEmpty()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("fail", "Nama produk tidak valid", null));
        }

        if (!authContext.isAuthenticated()) {
            return ResponseEntity.status(403).body(new ApiResponse<>("fail", "User tidak terautentikasi", null));
        }
        User authUser = authContext.getAuthUser();

        FoodProduct updatedProduct = foodQualityService.updateProduct(authUser.getId(), id, form);

        if (updatedProduct == null) {
            return ResponseEntity.status(404).body(new ApiResponse<>("fail", "Produk tidak ditemukan", null));
        }

        return ResponseEntity.ok(new ApiResponse<>("success", "Berhasil memperbarui data inspeksi", null));
    }

    // ==================================================================================
    // 6. UPDATE IMAGE - Mengubah HANYA Foto Sampel
    // ==================================================================================
    @PutMapping(value = "/{id}/image", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<ApiResponse<String>> updateProductImage(
            @PathVariable UUID id,
            @ModelAttribute ProductImageForm form) {

        // Validasi file menggunakan helper method di DTO
        if (form.isEmpty()) {
             return ResponseEntity.badRequest().body(new ApiResponse<>("fail", "File gambar tidak boleh kosong", null));
        }
        if (!form.isValidImage()) {
             return ResponseEntity.badRequest().body(new ApiResponse<>("fail", "Format file harus gambar (JPG/PNG)", null));
        }
        // Limit 5MB
        if (!form.isSizeValid(5 * 1024 * 1024)) {
             return ResponseEntity.badRequest().body(new ApiResponse<>("fail", "Ukuran file terlalu besar (Max 5MB)", null));
        }

        if (!authContext.isAuthenticated()) {
            return ResponseEntity.status(403).body(new ApiResponse<>("fail", "User tidak terautentikasi", null));
        }
        User authUser = authContext.getAuthUser();
        
        form.setId(id);

        boolean isUpdated = foodQualityService.updateProductImage(authUser.getId(), form);

        if (!isUpdated) {
            return ResponseEntity.status(404).body(new ApiResponse<>("fail", "Gagal update gambar. Produk tidak ditemukan.", null));
        }

        return ResponseEntity.ok(new ApiResponse<>("success", "Foto sampel berhasil diperbarui", null));
    }

    // ==================================================================================
    // 7. DELETE - Menghapus Data Produk
    // ==================================================================================
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(@PathVariable UUID id) {
        
        if (!authContext.isAuthenticated()) {
            return ResponseEntity.status(403).body(new ApiResponse<>("fail", "User tidak terautentikasi", null));
        }
        User authUser = authContext.getAuthUser();

        boolean status = foodQualityService.deleteProduct(authUser.getId(), id);
        if (!status) {
            return ResponseEntity.status(404).body(new ApiResponse<>("fail", "Produk tidak ditemukan", null));
        }

        return ResponseEntity.ok(new ApiResponse<>(
                "success",
                "Data produk berhasil dihapus",
                null));
    }

    // ==================================================================================
    // 8. STATS - Statistik Inspeksi (PASSED vs REJECTED)
    // ==================================================================================
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getQualityStats() {
        if (!authContext.isAuthenticated()) {
            return ResponseEntity.status(403).body(new ApiResponse<>("fail", "User tidak terautentikasi", null));
        }
        User authUser = authContext.getAuthUser();

        // Contoh return: { "PASSED": 150, "REJECTED": 5, "PENDING": 10 }
        Map<String, Long> stats = foodQualityService.getInspectionStats(authUser.getId());

        return ResponseEntity.ok(new ApiResponse<>(
                "success",
                "Berhasil mengambil statistik kualitas",
                stats));
    }
}