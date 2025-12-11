package org.delcom.app.dto;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class FoodProductFormTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testFoodProductForm_Valid() {
        FoodProductForm form = new FoodProductForm();
        form.setId(UUID.randomUUID());
        form.setProductName("Keripik Singkong");
        form.setBatchCode("BATCH-001"); // Field yang benar
        form.setCategory("Makanan Ringan");
        form.setInspectionStatus("PASSED");
        form.setNotes("Aman dikonsumsi");
        form.setProductionDate(LocalDate.now());
        form.setExpiryDate(LocalDate.now().plusMonths(6));

        // Test MultipartFile setter/getter
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        form.setImageFile(file);

        Set<ConstraintViolation<FoodProductForm>> violations = validator.validate(form);
        
        // Harusnya tidak ada error karena data lengkap
        assertTrue(violations.isEmpty(), "Form harusnya valid");
        
        // Test Getters
        assertEquals("Keripik Singkong", form.getProductName());
        assertEquals("BATCH-001", form.getBatchCode());
        assertEquals("Makanan Ringan", form.getCategory());
        assertEquals("PASSED", form.getInspectionStatus());
        assertNotNull(form.getId());
        assertEquals(file, form.getImageFile());
    }

    @Test
    void testFoodProductForm_Invalid_EmptyFields() {
        FoodProductForm form = new FoodProductForm();
        // Kosongkan field wajib (@NotBlank)
        form.setProductName(""); 
        form.setBatchCode(""); 

        Set<ConstraintViolation<FoodProductForm>> violations = validator.validate(form);

        // Harusnya ada 2 error (ProductName kosong & BatchCode kosong)
        assertEquals(2, violations.size());
    }

    @Test
    void testFoodProductForm_Invalid_NullFields() {
        FoodProductForm form = new FoodProductForm();
        // Null field wajib
        form.setProductName(null);
        form.setBatchCode(null);

        Set<ConstraintViolation<FoodProductForm>> violations = validator.validate(form);

        // Harusnya ada 2 error
        assertEquals(2, violations.size());
    }
        @Test
    void testProductImageGetterSetter() {
        // 1. Instansiasi Objek
        FoodProductForm form = new FoodProductForm();

        // 2. Test kondisi awal (biasanya null)
        assertNull(form.getProductImage());

        // 3. Panggil Setter (Ini akan membuat baris 'setProductImage' jadi Hijau)
        String filename = "contoh-gambar.jpg";
        form.setProductImage(filename);

        // 4. Panggil Getter & Validasi (Ini akan membuat baris 'getProductImage' jadi Hijau)
        assertEquals(filename, form.getProductImage());
    }
}