package org.delcom.app.dto;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.format.annotation.DateTimeFormat; // PENTING!

import java.time.LocalDate; // PENTING!
import java.util.UUID;

public class FoodProductForm {

    private UUID id;

    @NotBlank(message = "Kode Batch tidak boleh kosong")
    private String batchCode;

    @NotBlank(message = "Nama Produk tidak boleh kosong")
    private String productName;

    private String category;
    private String inspectionStatus;
    private String notes;

    // Field untuk upload gambar
    private MultipartFile imageFile;
    
    // Field untuk menyimpan nama file gambar lama (saat edit)
    private String productImage;

    // === [BARU] Tambahkan Field Tanggal ===
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    // ======================================
    // GETTER & SETTER (Wajib Ada)
    // ======================================

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getInspectionStatus() { return inspectionStatus; }
    public void setInspectionStatus(String inspectionStatus) { this.inspectionStatus = inspectionStatus; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public MultipartFile getImageFile() { return imageFile; }
    public void setImageFile(MultipartFile imageFile) { this.imageFile = imageFile; }

    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }

    // [BARU] Getter Setter Tanggal
    public LocalDate getProductionDate() { return productionDate; }
    public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
}