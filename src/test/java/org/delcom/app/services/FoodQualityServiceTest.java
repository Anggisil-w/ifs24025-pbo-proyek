package org.delcom.app.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

    import org.delcom.app.dto.FoodProductForm;
    import org.delcom.app.dto.ProductImageForm;
    import org.delcom.app.entities.FoodProduct;
import org.delcom.app.repositories.FoodQualityRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FoodQualityServiceTest {

    @Mock
    private FoodQualityRepository foodQualityRepository;

    @InjectMocks
    private FoodQualityService foodQualityService;

    private UUID userId;
    private UUID productId;
    private FoodProduct product;
    private FoodProductForm productForm;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();

        product = new FoodProduct();
        product.setId(productId);
        product.setUserId(userId);
        product.setProductName("Keripik Tempe");
        product.setBatchCode("B-001");
        product.setCategory("Snack");
        product.setInspectionStatus("PENDING");

        productForm = new FoodProductForm();
        productForm.setProductName("Keripik Tempe");
        productForm.setBatchCode("B-001");
        productForm.setCategory("Snack");
        productForm.setInspectionStatus("PENDING");
        productForm.setNotes("Baru");
    }

    // =========================================================================
    // 1. TEST CREATE
    // =========================================================================

    @Test
    @DisplayName("Create Product - Success with Image")
    void testCreateProduct_WithImage() {
        MockMultipartFile file = new MockMultipartFile(
                "imageFile", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes()
        );
        productForm.setImageFile(file);

        when(foodQualityRepository.save(any(FoodProduct.class))).thenReturn(product);

        FoodProduct created = foodQualityService.createProduct(userId, productForm);

        assertNotNull(created);
        verify(foodQualityRepository).save(any(FoodProduct.class));
    }

    @Test
    @DisplayName("Create Product - Success without Image")
    void testCreateProduct_NoImage() {
        productForm.setImageFile(null); // Tidak ada gambar

        when(foodQualityRepository.save(any(FoodProduct.class))).thenReturn(product);

        FoodProduct created = foodQualityService.createProduct(userId, productForm);

        assertNotNull(created);
        verify(foodQualityRepository).save(any(FoodProduct.class));
    }

    @Test
    @DisplayName("Create Product - Fail: IOException (File Error)")
    void testCreateProduct_IOException() throws IOException {
        // Mock File yang melempar IOException saat dibaca (simulasi error disk)
        MockMultipartFile badFile = spy(new MockMultipartFile(
                "imageFile", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes()
        ));
        
        doThrow(new IOException("Disk Error")).when(badFile).getInputStream();
        
        productForm.setImageFile(badFile);

        // Memastikan Service menangkap IOException dan melempar RuntimeException
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            foodQualityService.createProduct(userId, productForm);
        });

        // [PERBAIKAN UTAMA] Sesuaikan string dengan pesan error di Service (Bahasa Inggris)
        assertTrue(exception.getMessage().contains("Failed to store file"));
        
        verify(foodQualityRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Create Product - Image File Exists but Empty (0 bytes)")
    void testCreateProduct_ImageIsEmpty() {
        // 1. Buat Mock File yang TIDAK NULL, tapi kontennya KOSONG (byte[0])
        MockMultipartFile emptyFile = new MockMultipartFile(
                "imageFile", "test.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[0]
        );
        productForm.setImageFile(emptyFile);

        // 2. Mock Repository
        when(foodQualityRepository.save(any(FoodProduct.class))).thenReturn(product);

        // 3. Eksekusi Method
        FoodProduct created = foodQualityService.createProduct(userId, productForm);

        // 4. Verifikasi
        assertNotNull(created);
        // Pastikan repository tetap dipanggil (karena data produk tetap disimpan meski tanpa gambar)
        verify(foodQualityRepository).save(any(FoodProduct.class));
    }

    // =========================================================================
    // 2. TEST READ (GET ALL & SEARCH)
    // =========================================================================

    @Test
    @DisplayName("Get All Products - Without Parameter")
    void testGetAllProducts_NoParam() {
        when(foodQualityRepository.findByUserId(userId)).thenReturn(List.of(product));

        List<FoodProduct> result = foodQualityService.getAllProducts(userId);

        assertEquals(1, result.size());
        verify(foodQualityRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("Get All Products - With Search (Null/Empty)")
    void testGetAllProducts_SearchNullOrEmpty() {
        when(foodQualityRepository.findByUserId(userId)).thenReturn(List.of(product));

        // Test Null
        foodQualityService.getAllProducts(userId, null);
        // Test Empty
        foodQualityService.getAllProducts(userId, "");

        verify(foodQualityRepository, times(2)).findByUserId(userId);
        verify(foodQualityRepository, never()).findByUserIdWithSearch(any(), any());
    }

    @Test
    @DisplayName("Get All Products - With Search Value")
    void testGetAllProducts_WithSearch() {
        String keyword = "Keripik";
        when(foodQualityRepository.findByUserIdWithSearch(userId, keyword.toLowerCase()))
                .thenReturn(List.of(product));

        List<FoodProduct> result = foodQualityService.getAllProducts(userId, keyword);

        assertEquals(1, result.size());
        verify(foodQualityRepository).findByUserIdWithSearch(userId, keyword.toLowerCase());
    }

    // =========================================================================
    // 3. TEST READ (GET BY ID & BATCHES)
    // =========================================================================

    @Test
    @DisplayName("Get Product By ID - Found")
    void testGetProductById_Found() {
        when(foodQualityRepository.findByIdAndUserId(productId, userId)).thenReturn(Optional.of(product));

        FoodProduct result = foodQualityService.getProductById(userId, productId);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Get Product By ID - Not Found")
    void testGetProductById_NotFound() {
        when(foodQualityRepository.findByIdAndUserId(productId, userId)).thenReturn(Optional.empty());

        FoodProduct result = foodQualityService.getProductById(userId, productId);
        assertNull(result);
    }

    @Test
    @DisplayName("Get All Batch Codes")
    void testGetAllBatchCodes() {
        when(foodQualityRepository.findDistinctBatchCodesByUserId(userId))
                .thenReturn(List.of("B-001", "B-002"));

        List<String> batches = foodQualityService.getAllBatchCodes(userId);
        assertEquals(2, batches.size());
        assertEquals("B-001", batches.get(0));
    }

    // =========================================================================
    // 4. TEST UPDATE (TEXT & IMAGE)
    // =========================================================================

    @Test
    @DisplayName("Update Product - Success with New Image")
    void testUpdateProduct_WithImage() {
        MockMultipartFile file = new MockMultipartFile(
                "imageFile", "new.jpg", MediaType.IMAGE_JPEG_VALUE, "data".getBytes()
        );
        productForm.setImageFile(file);

        when(foodQualityRepository.findByIdAndUserId(productId, userId)).thenReturn(Optional.of(product));
        when(foodQualityRepository.save(any(FoodProduct.class))).thenAnswer(i -> i.getArguments()[0]);

        FoodProduct updated = foodQualityService.updateProduct(userId, productId, productForm);

        assertNotNull(updated);
        assertTrue(updated.getProductImage().contains("new.jpg"));
        verify(foodQualityRepository).save(product);
    }

    @Test
    @DisplayName("Update Product - Success without New Image")
    void testUpdateProduct_NoImage() {
        productForm.setImageFile(null); // Tidak update gambar

        when(foodQualityRepository.findByIdAndUserId(productId, userId)).thenReturn(Optional.of(product));
        when(foodQualityRepository.save(any(FoodProduct.class))).thenReturn(product);

        FoodProduct updated = foodQualityService.updateProduct(userId, productId, productForm);

        assertNotNull(updated);
        verify(foodQualityRepository).save(product);
    }
    
    @Test
    @DisplayName("Update Product - Image File Exists but Empty (0 bytes)")
    void testUpdateProduct_ImageIsEmpty() {
        // Mock File kosong
        MockMultipartFile emptyFile = new MockMultipartFile(
                "imageFile", "test.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[0]
        );
        productForm.setImageFile(emptyFile);

        when(foodQualityRepository.findByIdAndUserId(productId, userId)).thenReturn(Optional.of(product));
        when(foodQualityRepository.save(any(FoodProduct.class))).thenReturn(product);

        FoodProduct result = foodQualityService.updateProduct(userId, productId, productForm);

        assertNotNull(result);
        verify(foodQualityRepository).save(product);
    }

    @Test
    @DisplayName("Update Product - Not Found")
    void testUpdateProduct_NotFound() {
        when(foodQualityRepository.findByIdAndUserId(productId, userId)).thenReturn(Optional.empty());

        FoodProduct result = foodQualityService.updateProduct(userId, productId, productForm);
        assertNull(result);
        verify(foodQualityRepository, never()).save(any());
    }

    // =========================================================================
    // 5. TEST UPDATE IMAGE ONLY
    // =========================================================================

    @Test
    @DisplayName("Update Product Image Only - Success")
    void testUpdateProductImage_Success() {
        ProductImageForm imageForm = new ProductImageForm();
        imageForm.setId(productId);
        MockMultipartFile file = new MockMultipartFile(
                "imageFile", "only-image.jpg", "image/jpeg", "data".getBytes()
        );
        imageForm.setImageFile(file);

        when(foodQualityRepository.findByIdAndUserId(productId, userId)).thenReturn(Optional.of(product));

        boolean result = foodQualityService.updateProductImage(userId, imageForm);

        assertTrue(result);
        assertTrue(product.getProductImage().contains("only-image.jpg"));
        verify(foodQualityRepository).save(product);
    }

    @Test
    @DisplayName("Update Product Image Only - Not Found")
    void testUpdateProductImage_NotFound() {
        ProductImageForm imageForm = new ProductImageForm();
        imageForm.setId(productId);

        when(foodQualityRepository.findByIdAndUserId(productId, userId)).thenReturn(Optional.empty());

        boolean result = foodQualityService.updateProductImage(userId, imageForm);
        assertFalse(result);
    }

    @Test
    @DisplayName("Update Product Image Only - Empty Form")
    void testUpdateProductImage_EmptyForm() {
        ProductImageForm imageForm = new ProductImageForm();
        imageForm.setId(productId);
        imageForm.setImageFile(null); // Kosong

        when(foodQualityRepository.findByIdAndUserId(productId, userId)).thenReturn(Optional.of(product));

        boolean result = foodQualityService.updateProductImage(userId, imageForm);
        assertFalse(result);
        verify(foodQualityRepository, never()).save(any());
    }

    // =========================================================================
    // 6. TEST DELETE
    // =========================================================================

    @Test
    @DisplayName("Delete Product - Success")
    void testDeleteProduct_Success() {
        when(foodQualityRepository.findByIdAndUserId(productId, userId)).thenReturn(Optional.of(product));

        boolean result = foodQualityService.deleteProduct(userId, productId);

        assertTrue(result);
        verify(foodQualityRepository).deleteByIdAndUserId(productId, userId);
    }

    @Test
    @DisplayName("Delete Product - Not Found")
    void testDeleteProduct_NotFound() {
        when(foodQualityRepository.findByIdAndUserId(productId, userId)).thenReturn(Optional.empty());

        boolean result = foodQualityService.deleteProduct(userId, productId);

        assertFalse(result);
        verify(foodQualityRepository, never()).deleteByIdAndUserId(any(), any());
    }

    // =========================================================================
    // 7. TEST STATISTICS
    // =========================================================================

    @Test
    @DisplayName("Get Inspection Stats")
    void testGetInspectionStats() {
        when(foodQualityRepository.countByUserIdAndInspectionStatus(userId, "PASSED")).thenReturn(10L);
        when(foodQualityRepository.countByUserIdAndInspectionStatus(userId, "REJECTED")).thenReturn(0L); 
        when(foodQualityRepository.countByUserIdAndInspectionStatus(userId, "PENDING")).thenReturn(5L);

        Map<String, Long> stats = foodQualityService.getInspectionStats(userId);

        assertEquals(10L, stats.get("PASSED"));
        assertEquals(0L, stats.get("REJECTED"));
        assertEquals(5L, stats.get("PENDING"));
    }
    
    @Test
    @DisplayName("Get Inspection Stats - Values Are Null")
    void testGetInspectionStats_ValuesNull() {
        // Skenario return NULL (misal belum ada data sama sekali dan DB mengembalikan null untuk aggregate func)
        when(foodQualityRepository.countByUserIdAndInspectionStatus(userId, "PASSED")).thenReturn(null);
        when(foodQualityRepository.countByUserIdAndInspectionStatus(userId, "REJECTED")).thenReturn(null);
        when(foodQualityRepository.countByUserIdAndInspectionStatus(userId, "PENDING")).thenReturn(null);

        Map<String, Long> stats = foodQualityService.getInspectionStats(userId);

        // Verifikasi logic null-check di service (null ? ... : 0)
        assertEquals(0L, stats.get("PASSED"));
        assertEquals(0L, stats.get("REJECTED"));
        assertEquals(0L, stats.get("PENDING"));
    }

    // =========================================================================
    // 8. TEST DIRECTORY CREATION (REFLECTION)
    // =========================================================================
    
    @Test
    @DisplayName("Test Directory Creation When Not Exists")
    void testSaveFile_CreatesDirectory() throws IOException {
        // 1. Buat folder acak di target
        String tempDir = "target/temp_uploads_" + UUID.randomUUID() + "/";

        // 2. Ubah UPLOAD_DIR via Reflection
        ReflectionTestUtils.setField(foodQualityService, "UPLOAD_DIR", tempDir);

        // 3. Siapkan Mock Data
        MockMultipartFile file = new MockMultipartFile(
                "imageFile", "test-dir.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes()
        );
        productForm.setImageFile(file);

        when(foodQualityRepository.save(any(FoodProduct.class))).thenReturn(product);

        // 4. Eksekusi
        foodQualityService.createProduct(userId, productForm);

        // 5. Validasi
        Path path = Paths.get(tempDir);
        assertTrue(Files.exists(path), "Direktori baru harus dibuat otomatis");

        // 6. Cleanup
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(java.io.File::delete);
        }
    }
        @Test
    @DisplayName("Init - Fail to Initialize Storage (Force IOException)")
    void testInit_Failure() throws IOException {
        // 1. Tentukan path default (sesuai variabel UPLOAD_DIR di service, biasanya "uploads")
        Path conflictPath = Paths.get("uploads");

        // Bersihkan dulu jika ada sisa folder dari test lain
        if (Files.exists(conflictPath) && Files.isDirectory(conflictPath)) {
            // Hapus folder uploads agar kita bisa menempatkan file jebakan
            // Note: Hati-hati, ini menghapus folder uploads asli di project saat test jalan
            Files.walk(conflictPath)
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(java.io.File::delete);
        }

        // 2. Buat FILE (bukan folder) dengan nama "uploads"
        // Ini adalah "Jebakan". Saat service mencoba Files.createDirectories("uploads"),
        // dia akan gagal karena nama "uploads" sudah dipakai oleh file ini.
        if (!Files.exists(conflictPath)) {
            Files.createFile(conflictPath);
        }

        try {
            // 3. Action & Assert
            // Saat kita new FoodQualityService(), dia memanggil init() -> createDirectories()
            // Karena ada file "uploads", dia akan throw IOException -> ditangkap catch -> throw RuntimeException
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                new FoodQualityService(foodQualityRepository);
            });

            // Pastikan pesan error sesuai dengan yang ada di blok catch (Baris 40)
            assertTrue(exception.getMessage().contains("Could not initialize storage"));

        } finally {
            // 4. CLEANUP (Sangat Penting!)
            // Hapus file jebakan agar tidak mengganggu test lain atau aplikasi asli
            Files.deleteIfExists(conflictPath);
        }
    }
}