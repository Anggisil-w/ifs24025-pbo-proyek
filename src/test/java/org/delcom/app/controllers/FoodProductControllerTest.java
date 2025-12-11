package org.delcom.app.controllers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.delcom.app.configs.AuthContext;
import org.delcom.app.dto.FoodProductForm;
import org.delcom.app.dto.ProductImageForm;
import org.delcom.app.entities.FoodProduct;
import org.delcom.app.entities.User;
import org.delcom.app.services.FoodQualityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FoodProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FoodQualityService foodQualityService;

    @Mock
    private AuthContext authContext;

    @InjectMocks
    private FoodProductController controller;

    private User user;
    private FoodProduct product;
    private UUID userId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        // Inject AuthContext manual seperti contoh Anda
        ReflectionTestUtils.setField(controller, "authContext", authContext);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        userId = UUID.randomUUID();
        productId = UUID.randomUUID();

        user = mock(User.class);
        lenient().when(user.getId()).thenReturn(userId);

        product = mock(FoodProduct.class);
        lenient().when(product.getId()).thenReturn(productId);
        lenient().when(product.getProductName()).thenReturn("Keripik Tempe");
        lenient().when(product.getBatchCode()).thenReturn("BATCH-001");
        lenient().when(product.getInspectionStatus()).thenReturn("PASSED");
    }

    // ==================================================================================
    // 1. CREATE PRODUCT TESTS
    // ==================================================================================

    @Test
    void testCreate_Success() throws Exception {
        MockMultipartFile img = new MockMultipartFile(
                "imageFile", "sample.jpg",
                MediaType.IMAGE_JPEG_VALUE, "content".getBytes()
        );

        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(user);
        when(foodQualityService.createProduct(eq(userId), any(FoodProductForm.class))).thenReturn(product);

        mockMvc.perform(multipart("/api/food-products")
                        .file(img)
                        .param("productName", "Keripik Tempe")
                        .param("batchCode", "BATCH-001")
                        .param("inspectionStatus", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(productId.toString()));

        verify(foodQualityService).createProduct(eq(userId), any(FoodProductForm.class));
    }

@Test
    void testCreate_Unauthenticated() throws Exception {
        // Mock kondisi user belum login
        when(authContext.isAuthenticated()).thenReturn(false);

        MockMultipartFile img = new MockMultipartFile(
                "imageFile", "sample.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes()
        );

        // PERBAIKAN: Lengkapi semua parameter wajib (batchCode & inspectionStatus)
        // agar lolos validasi awal (HTTP 400) dan lanjut ke cek auth (HTTP 403)
        mockMvc.perform(multipart("/api/food-products")
                        .file(img)
                        .param("productName", "Valid Name")
                        .param("batchCode", "BATCH-TEST-001")      // <--- Tambahkan ini
                        .param("inspectionStatus", "PENDING"))     // <--- Tambahkan ini
                .andExpect(status().isForbidden())                 // Expect 403
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").value("User tidak terautentikasi"));

        verify(foodQualityService, never()).createProduct(any(), any());
    }

    @Test
    void testCreate_ProductNameNull() throws Exception {
        MockMultipartFile img = new MockMultipartFile(
                "imageFile", "sample.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes()
        );

        // Param productName tidak dikirim / null
        mockMvc.perform(multipart("/api/food-products")
                        .file(img)
                        // .param("productName", "...") <--- MISSING
                        .param("batchCode", "BATCH-001")
                        .param("inspectionStatus", "PENDING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.message").value("Nama produk tidak valid"));
    }

    @Test
    void testCreate_BatchCodeNull() throws Exception {
        MockMultipartFile img = new MockMultipartFile(
                "imageFile", "sample.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes()
        );

        mockMvc.perform(multipart("/api/food-products")
                        .file(img)
                        .param("productName", "Keripik")
                        // .param("batchCode", "...") <--- MISSING
                        .param("inspectionStatus", "PENDING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Kode batch tidak valid"));
    }

    @Test
    void testCreate_StatusNull() throws Exception {
        MockMultipartFile img = new MockMultipartFile(
                "imageFile", "sample.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes()
        );

        mockMvc.perform(multipart("/api/food-products")
                        .file(img)
                        .param("productName", "Keripik")
                        .param("batchCode", "BATCH-001"))
                        // .param("inspectionStatus", "...") <--- MISSING
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Status inspeksi tidak valid"));
    }

    @Test
    void testCreate_ImageFileIsNull() throws Exception {
        // TRICK: Kirim nama param yang salah ("random") agar DTO menerima null pada "imageFile"
        MockMultipartFile wrongFile = new MockMultipartFile(
                "random", "sample.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes()
        );

        mockMvc.perform(multipart("/api/food-products")
                        .file(wrongFile)
                        .param("productName", "Keripik")
                        .param("batchCode", "BATCH-001")
                        .param("inspectionStatus", "PENDING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Foto sampel produk wajib diupload"));
    }

    // ==================================================================================
    // 2. READ - GET ALL
    // ==================================================================================

    @Test
    void testGetAll_Success() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(user);
        when(foodQualityService.getAllProducts(userId, null)).thenReturn(List.of(product));

        mockMvc.perform(get("/api/food-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(foodQualityService).getAllProducts(userId, null);
    }

    @Test
    void testGetAll_Unauthenticated() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(false);

        mockMvc.perform(get("/api/food-products"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User tidak terautentikasi"));
    }

    // ==================================================================================
    // 3. READ - GET BY ID
    // ==================================================================================

    @Test
    void testGetById_Success() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(user);
        when(foodQualityService.getProductById(userId, productId)).thenReturn(product);

        mockMvc.perform(get("/api/food-products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void testGetById_NotFound() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(user);
        when(foodQualityService.getProductById(userId, productId)).thenReturn(null);

        mockMvc.perform(get("/api/food-products/" + productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Produk tidak ditemukan"));
    }

    @Test
    void testGetById_Unauthenticated() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(false);

        mockMvc.perform(get("/api/food-products/" + productId))
                .andExpect(status().isForbidden());
    }

    // ==================================================================================
    // 4. READ - GET BATCHES
    // ==================================================================================

    @Test
    void testGetBatches_Success() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(user);
        when(foodQualityService.getAllBatchCodes(userId)).thenReturn(List.of("B1", "B2"));

        mockMvc.perform(get("/api/food-products/batches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batches[0]").value("B1"));
    }

    @Test
    void testGetBatches_Unauthenticated() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(false);

        mockMvc.perform(get("/api/food-products/batches"))
                .andExpect(status().isForbidden());
    }

    // ==================================================================================
    // 5. UPDATE INFO
    // ==================================================================================

    @Test
    void testUpdate_Success() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(user);
        when(foodQualityService.updateProduct(eq(userId), eq(productId), any())).thenReturn(product);

        // Simulasi PUT dengan Multipart
        mockMvc.perform(multipart("/api/food-products/" + productId)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .param("productName", "New Name")
                        .param("batchCode", "B-NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Berhasil memperbarui data inspeksi"));
    }

@Test
    void testUpdate_ProductNameNull() throws Exception {
        MockMultipartFile emptyImage = new MockMultipartFile("imageFile", "", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/api/food-products/" + productId)
                        .file(emptyImage)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        // .param("productName", "...") <--- PARAMETER INI DIHAPUS/JANGAN DIKIRIM
                        .param("batchCode", "BATCH-UPDATED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nama produk tidak valid"));

        verify(foodQualityService, never()).updateProduct(any(), any(), any());
    }
    @Test
    void testUpdate_Unauthenticated() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(false);

        mockMvc.perform(multipart("/api/food-products/" + productId)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .param("productName", "Valid Name"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdate_NotFound() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(user);
        
        // Service return NULL
        when(foodQualityService.updateProduct(eq(userId), eq(productId), any())).thenReturn(null);

        mockMvc.perform(multipart("/api/food-products/" + productId)
                        .with(request -> { request.setMethod("PUT"); return request; })
                        .param("productName", "Valid Name"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Produk tidak ditemukan"));
    }

    // ==================================================================================
    // 6. UPDATE IMAGE
    // ==================================================================================

    @Test
    void testUpdateImage_Success() throws Exception {
        MockMultipartFile img = new MockMultipartFile(
                "imageFile", "new.jpg", MediaType.IMAGE_JPEG_VALUE, "new".getBytes()
        );

        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(user);
        when(foodQualityService.updateProductImage(eq(userId), any(ProductImageForm.class))).thenReturn(true);

        mockMvc.perform(multipart("/api/food-products/" + productId + "/image")
                        .file(img)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Foto sampel berhasil diperbarui"));
    }

    @Test
    void testUpdateImage_EmptyFile() throws Exception {
        // File ada tapi konten kosong (byte[0])
        MockMultipartFile emptyImg = new MockMultipartFile(
                "imageFile", "empty.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[0]
        );

        mockMvc.perform(multipart("/api/food-products/" + productId + "/image")
                        .file(emptyImg)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File gambar tidak boleh kosong"));
    }

    @Test
    void testUpdateImage_InvalidFormat() throws Exception {
        // Content type text/plain
        MockMultipartFile txtFile = new MockMultipartFile(
                "imageFile", "doc.txt", MediaType.TEXT_PLAIN_VALUE, "text".getBytes()
        );

        mockMvc.perform(multipart("/api/food-products/" + productId + "/image")
                        .file(txtFile)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Format file harus gambar (JPG/PNG)"));
    }

    @Test
    void testUpdateImage_FileTooLarge() throws Exception {
        // Buat dummy > 5MB
        byte[] largeContent = new byte[(5 * 1024 * 1024) + 10];
        MockMultipartFile largeFile = new MockMultipartFile(
                "imageFile", "large.jpg", MediaType.IMAGE_JPEG_VALUE, largeContent
        );

        mockMvc.perform(multipart("/api/food-products/" + productId + "/image")
                        .file(largeFile)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ukuran file terlalu besar (Max 5MB)"));
    }

    @Test
    void testUpdateImage_Unauthenticated() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(false);
        
        MockMultipartFile img = new MockMultipartFile(
                "imageFile", "new.jpg", MediaType.IMAGE_JPEG_VALUE, "data".getBytes()
        );

        mockMvc.perform(multipart("/api/food-products/" + productId + "/image")
                        .file(img)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateImage_NotFound() throws Exception {
        MockMultipartFile img = new MockMultipartFile(
                "imageFile", "new.jpg", MediaType.IMAGE_JPEG_VALUE, "data".getBytes()
        );

        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(user);
        
        // Service return FALSE
        when(foodQualityService.updateProductImage(eq(userId), any())).thenReturn(false);

        mockMvc.perform(multipart("/api/food-products/" + productId + "/image")
                        .file(img)
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Gagal update gambar. Produk tidak ditemukan."));
    }

    // ==================================================================================
    // 7. DELETE
    // ==================================================================================

    @Test
    void testDelete_Success() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(user);
        when(foodQualityService.deleteProduct(userId, productId)).thenReturn(true);

        mockMvc.perform(delete("/api/food-products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Data produk berhasil dihapus"));
    }

    @Test
    void testDelete_NotFound() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(user);
        when(foodQualityService.deleteProduct(userId, productId)).thenReturn(false);

        mockMvc.perform(delete("/api/food-products/" + productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Produk tidak ditemukan"));
    }

    @Test
    void testDelete_Unauthenticated() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(false);

        mockMvc.perform(delete("/api/food-products/" + productId))
                .andExpect(status().isForbidden());
    }

    // ==================================================================================
    // 8. STATS
    // ==================================================================================

    @Test
    void testStats_Success() throws Exception {
        Map<String, Long> stats = Map.of("PASSED", 10L, "REJECTED", 2L);

        when(authContext.isAuthenticated()).thenReturn(true);
        when(authContext.getAuthUser()).thenReturn(user);
        when(foodQualityService.getInspectionStats(userId)).thenReturn(stats);

        mockMvc.perform(get("/api/food-products/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.PASSED").value(10));
    }

    @Test
    void testStats_Unauthenticated() throws Exception {
        when(authContext.isAuthenticated()).thenReturn(false);

        mockMvc.perform(get("/api/food-products/stats"))
                .andExpect(status().isForbidden());
    }
    @Test
    void testCreate_ProductNameEmpty() throws Exception {
        MockMultipartFile img = new MockMultipartFile(
                "imageFile", "sample.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes()
        );

        // Validasi jalan sebelum Auth, tapi kita mock saja biar aman
        // when(authContext.isAuthenticated()).thenReturn(true); 

        mockMvc.perform(multipart("/api/food-products")
                        .file(img)
                        .param("productName", "") // <--- INPUT STRING KOSONG
                        .param("batchCode", "BATCH-001")
                        .param("inspectionStatus", "PENDING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nama produk tidak valid"));
    }
    @Test
    void testCreate_BatchCodeEmpty() throws Exception {
        MockMultipartFile img = new MockMultipartFile(
                "imageFile", "sample.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes()
        );

        mockMvc.perform(multipart("/api/food-products")
                        .file(img)
                        .param("productName", "Keripik")
                        .param("batchCode", "") // <--- INPUT STRING KOSONG
                        .param("inspectionStatus", "PENDING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Kode batch tidak valid"));
    }
    @Test
    void testCreate_ImageFileIsEmptyContent() throws Exception {
        // Membuat file dengan nama valid, TAPI konten byte kosong (new byte[0])
        MockMultipartFile emptyContentFile = new MockMultipartFile(
                "imageFile", "sample.jpg", 
                MediaType.IMAGE_JPEG_VALUE, 
                new byte[0] // <--- KONTEN KOSONG (Size 0)
        );

        mockMvc.perform(multipart("/api/food-products")
                        .file(emptyContentFile)
                        .param("productName", "Keripik")
                        .param("batchCode", "BATCH-001")
                        .param("inspectionStatus", "PENDING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Foto sampel produk wajib diupload"));
    }
    @Test
    void testCreate_InspectionStatusEmpty() throws Exception {
        MockMultipartFile img = new MockMultipartFile(
                "imageFile", "sample.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes()
        );

        mockMvc.perform(multipart("/api/food-products")
                        .file(img)
                        .param("productName", "Keripik")
                        .param("batchCode", "BATCH-001")
                        .param("inspectionStatus", "")) // <--- INPUT STRING KOSONG
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Status inspeksi tidak valid"));
    }
    @Test
    void testUpdate_ProductNameEmpty() throws Exception {
        MockMultipartFile emptyImage = new MockMultipartFile("imageFile", "", "application/octet-stream", new byte[0]);

        // Kita tidak perlu mock Auth atau Service karena validasi gagal di awal
        // when(authContext.isAuthenticated()).thenReturn(true); 

        mockMvc.perform(multipart("/api/food-products/" + productId)
                        .file(emptyImage)
                        .with(request -> { request.setMethod("PUT"); return request; }) // Ubah method jadi PUT
                        .param("productName", "") // <--- INPUT STRING KOSONG
                        .param("batchCode", "BATCH-UPDATED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Nama produk tidak valid"));

        verify(foodQualityService, never()).updateProduct(any(), any(), any());
    }
}