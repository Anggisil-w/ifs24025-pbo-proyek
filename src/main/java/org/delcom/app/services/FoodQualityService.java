package org.delcom.app.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.delcom.app.dto.FoodProductForm;
import org.delcom.app.dto.ProductImageForm;
import org.delcom.app.entities.FoodProduct;
import org.delcom.app.repositories.FoodQualityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class FoodQualityService {

    private final FoodQualityRepository productRepository;
    
    // Default path. Test akan mengubah field ini via Reflection,
    // jadi kita membacanya secara dinamis di method storeFile.
    private String UPLOAD_DIR = "uploads"; 

    public FoodQualityService(FoodQualityRepository productRepository) {
        this.productRepository = productRepository;
        init();
    }

    private void init() {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    public List<FoodProduct> getAllProducts(UUID userId, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return productRepository.findByUserIdWithSearch(userId, keyword.toLowerCase());
        }
        return productRepository.findByUserId(userId);
    }
    
    public List<FoodProduct> getAllProducts(UUID userId) {
        return getAllProducts(userId, null);
    }

    public List<String> getAllBatchCodes(UUID userId) {
        return productRepository.findDistinctBatchCodesByUserId(userId);
    }

    public Map<String, Long> getInspectionStats(UUID userId) {
        Long passed = productRepository.countByUserIdAndInspectionStatus(userId, "PASSED");
        Long rejected = productRepository.countByUserIdAndInspectionStatus(userId, "REJECTED");
        Long pending = productRepository.countByUserIdAndInspectionStatus(userId, "PENDING");
        
        long safePassed = (passed != null) ? passed : 0L;
        long safeRejected = (rejected != null) ? rejected : 0L;
        long safePending = (pending != null) ? pending : 0L;
        long total = safePassed + safeRejected + safePending;

        Map<String, Long> stats = new HashMap<>();
        stats.put("PASSED", safePassed);
        stats.put("REJECTED", safeRejected);
        stats.put("PENDING", safePending);
        stats.put("TOTAL", total);
        
        return stats;
    }

    public FoodProduct createProduct(UUID userId, FoodProductForm form) {
        FoodProduct product = new FoodProduct();
        product.setUserId(userId);
        product.setBatchCode(form.getBatchCode());
        product.setProductName(form.getProductName());
        product.setCategory(form.getCategory());
        product.setInspectionStatus(form.getInspectionStatus());
        product.setNotes(form.getNotes());
        
        product.setProductionDate(form.getProductionDate());
        product.setExpiryDate(form.getExpiryDate());

        // Jika upload gagal, Exception akan dilempar dan ditangkap oleh Test
        if (form.getImageFile() != null && !form.getImageFile().isEmpty()) {
            String filename = storeFile(form.getImageFile());
            product.setProductImage(filename);
        }

        return productRepository.save(product);
    }

    public FoodProduct updateProduct(UUID userId, UUID productId, FoodProductForm form) {
        FoodProduct product = productRepository.findByIdAndUserId(productId, userId).orElse(null);
        if (product != null) {
            product.setProductName(form.getProductName());
            product.setBatchCode(form.getBatchCode());
            product.setCategory(form.getCategory());
            product.setInspectionStatus(form.getInspectionStatus());
            product.setNotes(form.getNotes());
            
            product.setProductionDate(form.getProductionDate());
            product.setExpiryDate(form.getExpiryDate());

            if (form.getImageFile() != null && !form.getImageFile().isEmpty()) {
                String filename = storeFile(form.getImageFile());
                product.setProductImage(filename);
            }

            return productRepository.save(product);
        }
        return null;
    }

    public FoodProduct getProductById(UUID userId, UUID productId) {
        return productRepository.findByIdAndUserId(productId, userId).orElse(null);
    }

    public boolean deleteProduct(UUID userId, UUID productId) {
        if (productRepository.findByIdAndUserId(productId, userId).isPresent()) {
            productRepository.deleteByIdAndUserId(productId, userId);
            return true;
        }
        return false;
    }

    public boolean updateProductImage(UUID userId, ProductImageForm form) {
         FoodProduct product = productRepository.findByIdAndUserId(form.getId(), userId).orElse(null);
         if (product != null && form.getImageFile() != null) {
             String filename = storeFile(form.getImageFile());
             product.setProductImage(filename);
             productRepository.save(product);
             return true;
         }
         return false;
    }

    private String storeFile(MultipartFile file) {
        try {
            // Mengambil path secara dinamis untuk mendukung Test Environment
            Path root = Paths.get(UPLOAD_DIR);
            
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }

            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path targetLocation = root.resolve(filename);
            
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            return filename;
        } catch (IOException e) {
            // PERBAIKAN UTAMA:
            // 1. Menggunakan pesan bahasa Inggris "Failed to store file" agar sesuai ekspektasi Test.
            // 2. Menyertakan 'e' sebagai cause agar pengecekan (e.getCause() instanceof IOException) berhasil.
            throw new RuntimeException("Failed to store file " + file.getOriginalFilename(), e);
        }
    }
}