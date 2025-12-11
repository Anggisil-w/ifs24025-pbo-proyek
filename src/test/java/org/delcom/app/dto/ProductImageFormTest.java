package org.delcom.app.dto;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class ProductImageFormTest {

    // ==================================================================================
    // 1. Test Getters & Setters
    // ==================================================================================
    @Test
    @DisplayName("Test Getters and Setters")
    void testGettersAndSetters() {
        ProductImageForm form = new ProductImageForm();
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "imageFile", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes()
        );

        form.setId(id);
        form.setImageFile(file);

        assertEquals(id, form.getId());
        assertEquals(file, form.getImageFile());
    }

    // ==================================================================================
    // 2. Test isEmpty()
    // ==================================================================================
    @Test
    @DisplayName("Test isEmpty()")
    void testIsEmpty() {
        ProductImageForm form = new ProductImageForm();

        // 1. Kondisi File NULL
        form.setImageFile(null);
        assertTrue(form.isEmpty(), "Harus true jika file null");

        // 2. Kondisi File Kosong (byte[0])
        MockMultipartFile emptyFile = new MockMultipartFile(
                "imageFile", "", MediaType.IMAGE_JPEG_VALUE, new byte[0]
        );
        form.setImageFile(emptyFile);
        assertTrue(form.isEmpty(), "Harus true jika file kosong (0 bytes)");

        // 3. Kondisi File Ada Isi
        MockMultipartFile validFile = new MockMultipartFile(
                "imageFile", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "data".getBytes()
        );
        form.setImageFile(validFile);
        assertFalse(form.isEmpty(), "Harus false jika file ada isinya");
    }

    // ==================================================================================
    // 3. Test getOriginalFilename()
    // ==================================================================================
    @Test
    @DisplayName("Test getOriginalFilename()")
    void testGetOriginalFilename() {
        ProductImageForm form = new ProductImageForm();

        // 1. Kondisi File NULL
        form.setImageFile(null);
        assertNull(form.getOriginalFilename(), "Harus return null jika file null");

        // 2. Kondisi File Ada
        String filename = "my-photo.png";
        MockMultipartFile file = new MockMultipartFile(
                "imageFile", filename, MediaType.IMAGE_PNG_VALUE, "content".getBytes()
        );
        form.setImageFile(file);
        assertEquals(filename, form.getOriginalFilename());
    }

    // ==================================================================================
    // 4. Test isValidImage() - Content Type Check
    // ==================================================================================
    @Test
    @DisplayName("Test isValidImage() with various content types")
    void testIsValidImage() {
        ProductImageForm form = new ProductImageForm();

        // 1. Test jika File NULL atau Empty -> False
        form.setImageFile(null);
        assertFalse(form.isValidImage());

        // 2. Test Tipe Valid (JPEG, PNG, JPG, GIF, WEBP)
        String[] validTypes = {
            "image/jpeg", 
            "image/png", 
            "image/jpg", 
            "image/gif", 
            "image/webp"
        };

        for (String type : validTypes) {
            MockMultipartFile file = new MockMultipartFile(
                    "imageFile", "pic", type, "content".getBytes()
            );
            form.setImageFile(file);
            assertTrue(form.isValidImage(), "Seharusnya TRUE untuk tipe: " + type);
        }

        // 3. Test Tipe Invalid (Misal: Text, PDF)
        MockMultipartFile invalidFile = new MockMultipartFile(
                "imageFile", "doc.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes()
        );
        form.setImageFile(invalidFile);
        assertFalse(form.isValidImage(), "Seharusnya FALSE untuk tipe text/plain");
    }

    @Test
    @DisplayName("Test isValidImage() when ContentType is NULL")
    void testIsValidImage_NullContentType() {
        ProductImageForm form = new ProductImageForm();
        
        // Mock file tanpa content type
        MockMultipartFile file = new MockMultipartFile(
                "imageFile", "pic", null, "content".getBytes()
        );
        form.setImageFile(file);
        
        assertFalse(form.isValidImage(), "Seharusnya FALSE jika content type null");
    }

    // ==================================================================================
    // 5. Test isSizeValid()
    // ==================================================================================
    @Test
    @DisplayName("Test isSizeValid()")
    void testIsSizeValid() {
        ProductImageForm form = new ProductImageForm();
        long maxSize = 100; // 100 bytes

        // 1. Test jika File NULL -> False
        form.setImageFile(null);
        assertFalse(form.isSizeValid(maxSize));

        // 2. Test Ukuran Lebih Kecil (Valid)
        MockMultipartFile smallFile = new MockMultipartFile(
                "imageFile", "s.jpg", "image/jpeg", new byte[50]
        );
        form.setImageFile(smallFile);
        assertTrue(form.isSizeValid(maxSize));

        // 3. Test Ukuran Sama Persis (Valid Boundary)
        MockMultipartFile exactFile = new MockMultipartFile(
                "imageFile", "e.jpg", "image/jpeg", new byte[100]
        );
        form.setImageFile(exactFile);
        assertTrue(form.isSizeValid(maxSize));

        // 4. Test Ukuran Lebih Besar (Invalid)
        MockMultipartFile largeFile = new MockMultipartFile(
                "imageFile", "l.jpg", "image/jpeg", new byte[101]
        );
        form.setImageFile(largeFile);
        assertFalse(form.isSizeValid(maxSize));
    }
}