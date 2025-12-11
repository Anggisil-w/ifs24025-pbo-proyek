package org.delcom.app.repositories;

import org.delcom.app.entities.FoodProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FoodQualityRepository extends JpaRepository<FoodProduct, UUID> {

    // 1. Cari berdasarkan User ID (Standard)
    List<FoodProduct> findByUserId(UUID userId);

    // 2. Cari berdasarkan ID dan User ID (Untuk Edit/Delete yang aman)
    Optional<FoodProduct> findByIdAndUserId(UUID id, UUID userId);

    // 3. Hapus berdasarkan ID dan User ID
    void deleteByIdAndUserId(UUID id, UUID userId);

    // 4. Hitung jumlah berdasarkan Status Inspeksi (Untuk Chart: PASSED vs REJECTED)
    // Sesuai dengan nama field di entity 'inspectionStatus'
    Long countByUserIdAndInspectionStatus(UUID userId, String inspectionStatus);

    // 5. Query Pencarian Custom (Untuk Fitur Search)
    // Mencari di kolom productName, batchCode, atau category
    @Query("SELECT f FROM FoodProduct f WHERE f.userId = :userId AND " +
           "(LOWER(f.productName) LIKE %:keyword% OR " +
           "LOWER(f.batchCode) LIKE %:keyword% OR " +
           "LOWER(f.category) LIKE %:keyword%)")
    List<FoodProduct> findByUserIdWithSearch(@Param("userId") UUID userId, @Param("keyword") String keyword);

    // 6. Ambil list Kode Batch unik (Untuk Filter Dropdown)
    // Digunakan di Controller endpoint /batches
    @Query("SELECT DISTINCT f.batchCode FROM FoodProduct f WHERE f.userId = :userId ORDER BY f.batchCode")
    List<String> findDistinctBatchCodesByUserId(@Param("userId") UUID userId);
}