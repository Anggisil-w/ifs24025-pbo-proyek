package org.delcom.app.dto;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotNull;

public class ProductImageForm {

    private UUID id;

    // Digunakan untuk menangkap file upload saat update foto sampel produk
    @NotNull(message = "File gambar sampel tidak boleh kosong")
    private MultipartFile imageFile; 

    // Constructor
    public ProductImageForm() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public MultipartFile getImageFile() {
        return imageFile;
    }

    public void setImageFile(MultipartFile imageFile) {
        this.imageFile = imageFile;
    }

    // --- Helper Methods (Validasi File) ---

    // Cek apakah file kosong
    public boolean isEmpty() {
        return imageFile == null || imageFile.isEmpty();
    }

    // Ambil nama file asli
    public String getOriginalFilename() {
        return imageFile != null ? imageFile.getOriginalFilename() : null;
    }

    // Validasi Tipe File (Harus Gambar: JPG, PNG, GIF, WEBP)
    public boolean isValidImage() {
        if (this.isEmpty()) {
            return false;
        }

        String contentType = imageFile.getContentType();
        return contentType != null &&
                (contentType.equals("image/jpeg") ||
                 contentType.equals("image/png") ||
                 contentType.equals("image/jpg") || 
                 contentType.equals("image/gif") ||
                 contentType.equals("image/webp"));
    }

    // Validasi Ukuran File (Contoh: max 2MB atau 5MB)
    public boolean isSizeValid(long maxSize) {
        return imageFile != null && imageFile.getSize() <= maxSize;
    }
}