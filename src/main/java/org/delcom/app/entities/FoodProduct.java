package org.delcom.app.entities;

import java.time.LocalDate;
import java.time.LocalDateTime; // [BARU] Import LocalDate
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

    // --- 1. ID (Wajib) ---
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    // --- 2. User ID ---
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // --- 3. Kode Batch ---
    @Column(name = "batch_code", nullable = false, unique = true)
    private String batchCode;

    // --- 4. Nama Produk ---
    @Column(name = "product_name", nullable = false)
    private String productName;

    // --- 5. Kategori ---
    @Column(name = "category", nullable = false)
    private String category;

    // --- 6. Status Inspeksi ---
    @Column(name = "inspection_status", nullable = false)
    private String inspectionStatus;

    // --- 7. Foto Sampel ---
    @Column(name = "product_image", nullable = true)
    private String productImage;

    // --- 8. Catatan ---
    @Column(name = "notes", nullable = true, length = 1000) // length opsional, untuk teks panjang
    private String notes;
    
    // --- [BARU] 9. Tanggal Produksi ---
    @Column(name = "production_date")
    private LocalDate productionDate;

    // --- [BARU] 10. Tanggal Kedaluwarsa ---
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // --- 11. Created At ---
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // --- 12. Updated At ---
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ======= Constructor =======
    public FoodProduct() {
    }

    public FoodProduct(UUID userId, String batchCode, String productName, String category, String inspectionStatus, String notes) {
        this.userId = userId;
        this.batchCode = batchCode;
        this.productName = productName;
        this.category = category;
        this.inspectionStatus = inspectionStatus;
        this.notes = notes;
    }

    // ======= Getter & Setter =======
    
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getBatchCode() {
        return batchCode;
    }

    public void setBatchCode(String batchCode) {
        this.batchCode = batchCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getInspectionStatus() {
        return inspectionStatus;
    }

    public void setInspectionStatus(String inspectionStatus) {
        this.inspectionStatus = inspectionStatus;
    }

    public String getProductImage() {
        return productImage;
    }

    public void setProductImage(String productImage) {
        this.productImage = productImage;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // [BARU] Getter Setter Tanggal
    public LocalDate getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(LocalDate productionDate) {
        this.productionDate = productionDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }
    // [AKHIR BARU]

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ======= @PrePersist & @PreUpdate =======
    @PrePersist
    protected void onCreate() {
    LocalDateTime now = LocalDateTime.now(); // Ambil waktu satu kali saja
    this.createdAt = now;
    this.updatedAt = now; // Gunakan variabel yang sama
}
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}