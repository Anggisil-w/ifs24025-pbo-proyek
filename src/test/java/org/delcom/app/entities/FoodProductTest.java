package org.delcom.app.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class FoodProductTest {

    // ==================================================================================
    // 1. Test Constructor & Getters
    // ==================================================================================
    @Test
    @DisplayName("Test Custom Constructor and Getters")
    void testConstructorAndGetters() {
        UUID userId = UUID.randomUUID();
        String batchCode = "BATCH-001";
        String productName = "Keripik Singkong";
        String category = "Snack";
        String inspectionStatus = "PASSED";
        String notes = "Renyah dan gurih";

        // Menggunakan Constructor Custom
        FoodProduct product = new FoodProduct(
                userId, batchCode, productName, category, inspectionStatus, notes
        );

        // Validasi nilai
        assertEquals(userId, product.getUserId());
        assertEquals(batchCode, product.getBatchCode());
        assertEquals(productName, product.getProductName());
        assertEquals(category, product.getCategory());
        assertEquals(inspectionStatus, product.getInspectionStatus());
        assertEquals(notes, product.getNotes());
    }

    // ==================================================================================
    // 2. Test Setters
    // ==================================================================================
    @Test
    @DisplayName("Test Setters")
    void testSetters() {
        FoodProduct product = new FoodProduct();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Set Values
        product.setId(id);
        product.setUserId(userId);
        product.setBatchCode("B-99");
        product.setProductName("Susu UHT");
        product.setCategory("Minuman");
        product.setInspectionStatus("PENDING");
        product.setProductImage("image.jpg");
        product.setNotes("Segar");

        // Assert Values
        assertEquals(id, product.getId());
        assertEquals(userId, product.getUserId());
        assertEquals("B-99", product.getBatchCode());
        assertEquals("Susu UHT", product.getProductName());
        assertEquals("Minuman", product.getCategory());
        assertEquals("PENDING", product.getInspectionStatus());
        assertEquals("image.jpg", product.getProductImage());
        assertEquals("Segar", product.getNotes());
    }

    // ==================================================================================
    // 3. Test Lifecycle Methods (@PrePersist - onCreate)
    // ==================================================================================
    @Test
    @DisplayName("Test @PrePersist (onCreate) sets ID and Time")
    void testPrePersist() {
        FoodProduct product = new FoodProduct();

        // Kondisi Awal: ID dan Waktu harus NULL
        assertNull(product.getId(), "ID awal harus null");
        assertNull(product.getCreatedAt());
        assertNull(product.getUpdatedAt());

        // ACTION: Panggil method 'onCreate'
        // Kita pakai ReflectionTestUtils karena method onCreate biasanya protected/private
        ReflectionTestUtils.invokeMethod(product, "onCreate");

        // VALIDASI:
        // 1. ID harus ter-generate otomatis (sesuai logika Entity yang baru)
        assertNotNull(product.getId(), "ID harus digenerate saat onCreate");
        
        // 2. Waktu harus terisi
        assertNotNull(product.getCreatedAt());
        assertNotNull(product.getUpdatedAt());

        // 3. Waktu create dan update harus sama saat pertama kali dibuat
        assertEquals(product.getCreatedAt(), product.getUpdatedAt());
    }

    // ==================================================================================
    // 4. Test Lifecycle Methods (@PreUpdate - onUpdate)
    // ==================================================================================
    @Test
    @DisplayName("Test @PreUpdate (onUpdate) updates updatedAt only")
    void testPreUpdate() throws InterruptedException {
        FoodProduct product = new FoodProduct();

        // 1. Simulasikan Create dulu
        ReflectionTestUtils.invokeMethod(product, "onCreate");
        LocalDateTime createdTime = product.getCreatedAt();
        LocalDateTime initialUpdatedTime = product.getUpdatedAt();

        assertNotNull(createdTime);

        // 2. Beri jeda sedikit (10ms) agar waktu sistem berubah
        Thread.sleep(10);

        // 3. Simulasikan Update
        ReflectionTestUtils.invokeMethod(product, "onUpdate");

        // 4. Validasi
        // createdAt TIDAK boleh berubah
        assertEquals(createdTime, product.getCreatedAt());

        // updatedAt HARUS berubah (lebih baru dari waktu awal)
        assertTrue(product.getUpdatedAt().isAfter(initialUpdatedTime));
        assertNotEquals(product.getCreatedAt(), product.getUpdatedAt());
    }

    // ==================================================================================
    // 5. Test Date Fields (Production & Expiry)
    // ==================================================================================
    @Test
    @DisplayName("Test Date Fields (Production & Expiry)")
    void testDateFields() {
        FoodProduct product = new FoodProduct();
        
        // Buat dummy tanggal
        LocalDate prodDate = LocalDate.of(2023, 10, 1);
        LocalDate expDate = LocalDate.of(2024, 10, 1);

        // 1. Eksekusi Setter (Menghijaukan method set...)
        product.setProductionDate(prodDate);
        product.setExpiryDate(expDate);

        // 2. Eksekusi Getter & Validasi (Menghijaukan method get...)
        assertEquals(prodDate, product.getProductionDate());
        assertEquals(expDate, product.getExpiryDate());
    }
}