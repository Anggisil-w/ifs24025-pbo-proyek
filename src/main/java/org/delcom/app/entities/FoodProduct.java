package org.delcom.app.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "food_products")
public class FoodProduct {

    // --- 1. ID ---
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    // ... (field lainnya userId, batchCode, dll TETAP SAMA seperti kode Anda) ...
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "batch_code", nullable = false, unique = true)
    private String batchCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "inspection_status", nullable = false)
    private String inspectionStatus;

    @Column(name = "product_image", nullable = true)
    private String productImage;

    @Column(name = "notes", nullable = true, length = 1000)
    private String notes;
    
    @Column(name = "production_date")
    private LocalDate productionDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ======= Constructor =======
    public FoodProduct() {}

    public FoodProduct(UUID userId, String batchCode, String productName, String category, String inspectionStatus, String notes) {
        this.userId = userId;
        this.batchCode = batchCode;
        this.productName = productName;
        this.category = category;
        this.inspectionStatus = inspectionStatus;
        this.notes = notes;
    }

    // ======= Getter & Setter (TETAP SAMA) =======
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getBatchCode() { return batchCode; }
    public void setBatchCode(String batchCode) { this.batchCode = batchCode; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getInspectionStatus() { return inspectionStatus; }
    public void setInspectionStatus(String inspectionStatus) { this.inspectionStatus = inspectionStatus; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDate getProductionDate() { return productionDate; }
    public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ======= PERBAIKAN UTAMA DI SINI =======
    
    // Ubah visibility menjadi public agar bisa diakses Unit Test dengan mudah
    @PrePersist
    public void onCreate() {
        // Logika: Jika ID belum ada (Unit Test), buatkan manual.
        // Jika pakai Hibernate, ini juga aman karena Hibernate cek ID sebelum insert.
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        
        // Set waktu otomatis
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}