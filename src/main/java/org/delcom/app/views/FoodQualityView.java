package org.delcom.app.views;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.delcom.app.dto.ProductImageForm;
import org.delcom.app.dto.FoodProductForm;
import org.delcom.app.entities.FoodProduct;
import org.delcom.app.entities.User;
import org.delcom.app.services.FoodQualityService;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/food-products")
public class FoodQualityView {

    private final FoodQualityService foodQualityService;
    private final Path rootLocation = Paths.get("uploads");

    public FoodQualityView(FoodQualityService foodQualityService) {
        this.foodQualityService = foodQualityService;
    }

    // ==========================================================
    // 1. DASHBOARD (INDEX)
    // ==========================================================
    @GetMapping
    public String index(Model model, @RequestParam(required = false) String search) {
        User authUser = getAuthUser();
        if (authUser == null) return "redirect:/auth/login";
        model.addAttribute("auth", authUser);

        List<FoodProduct> allProducts = foodQualityService.getAllProducts(authUser.getId());

        // --- STATISTIK ---
        long countPassed = allProducts.stream().filter(s -> "PASSED".equalsIgnoreCase(s.getInspectionStatus())).count();
        long countRejected = allProducts.stream().filter(s -> "REJECTED".equalsIgnoreCase(s.getInspectionStatus())).count();
        long countPending = allProducts.stream().filter(s -> "PENDING".equalsIgnoreCase(s.getInspectionStatus())).count();

        long countMakanan = allProducts.stream().filter(s -> "Makanan Ringan".equalsIgnoreCase(s.getCategory())).count();
        long countMinuman = allProducts.stream().filter(s -> "Minuman".equalsIgnoreCase(s.getCategory())).count();
        long countBahanBaku = allProducts.stream().filter(s -> "Bahan Baku".equalsIgnoreCase(s.getCategory())).count();

        model.addAttribute("countPassed", countPassed);
        model.addAttribute("countRejected", countRejected);
        model.addAttribute("countPending", countPending);
        
        model.addAttribute("countMakanan", countMakanan);
        model.addAttribute("countMinuman", countMinuman);
        model.addAttribute("countBahanBaku", countBahanBaku);
        // ----------------

        // Search Logic
        List<FoodProduct> displayList = allProducts;
        if (search != null && !search.isBlank()) {
            String keyword = search.toLowerCase();
            displayList = allProducts.stream()
                .filter(p -> p.getProductName().toLowerCase().contains(keyword) || 
                             p.getBatchCode().toLowerCase().contains(keyword) ||
                             p.getCategory().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
            model.addAttribute("search", search);
        }

        model.addAttribute("foodProducts", displayList);
        return "pages/food-products/home"; 
    }

    // ==========================================================
    // 2. FITUR ADD (SEPARATE PAGE)
    // ==========================================================
    
    // [BARU] Menampilkan Halaman Form Tambah
    @GetMapping("/add")
    public String showAddPage(Model model) {
        if (getAuthUser() == null) return "redirect:/auth/login";
        
        // Siapkan form kosong
        model.addAttribute("foodProductForm", new FoodProductForm());
        
        return "models/food-products/add"; // Mengarah ke add.html
    }

    // Memproses POST Data Tambah
    @PostMapping("/add")
    public String postAddProduct(@Valid @ModelAttribute("foodProductForm") FoodProductForm form,
            RedirectAttributes redirectAttributes) {

        User authUser = getAuthUser();
        if (authUser == null) return "redirect:/auth/login";

        // Validasi Manual
        if (form.getProductName() == null || form.getProductName().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Nama Produk wajib diisi");
            return "redirect:/food-products/add"; // Balik ke form add jika error
        }
        if (form.getImageFile() == null || form.getImageFile().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Foto sampel wajib diupload");
            return "redirect:/food-products/add";
        }

        try {
            var entity = foodQualityService.createProduct(authUser.getId(), form);
            if (entity != null) {
                redirectAttributes.addFlashAttribute("success", "Produk berhasil didaftarkan.");
                return "redirect:/food-products"; // Sukses -> Dashboard
            }
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "Gagal: Kode Batch sudah digunakan.");
            return "redirect:/food-products/add";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/food-products/add";
        }
        
        redirectAttributes.addFlashAttribute("error", "Gagal menyimpan data.");
        return "redirect:/food-products/add";
    }

    // ==========================================================
    // 3. FITUR EDIT DATA (SEPARATE PAGE)
    // ==========================================================

    // [BARU] Menampilkan Halaman Edit Text
@GetMapping("/edit/{id}")
    public String showEditPage(@PathVariable UUID id, Model model) {
        User authUser = getAuthUser();
        if (authUser == null) return "redirect:/auth/login";

        FoodProduct product = foodQualityService.getProductById(authUser.getId(), id);
        if (product == null) return "redirect:/food-products";

        FoodProductForm form = new FoodProductForm();
        form.setId(product.getId());
        form.setBatchCode(product.getBatchCode());
        form.setProductName(product.getProductName());
        form.setCategory(product.getCategory());
        form.setInspectionStatus(product.getInspectionStatus());
        form.setNotes(product.getNotes());
        form.setProductImage(product.getProductImage()); // Agar gambar lama muncul
        
        // [BARU] Copy Tanggal dari Entity ke Form
        form.setProductionDate(product.getProductionDate());
        form.setExpiryDate(product.getExpiryDate());

        model.addAttribute("foodProductForm", form);
        return "models/food-products/edit"; 
    }

    // Memproses POST Edit
    @PostMapping("/edit")
    public String postEditProduct(@Valid @ModelAttribute("foodProductForm") FoodProductForm form,
            RedirectAttributes redirectAttributes) {

        User authUser = getAuthUser();
        if (authUser == null) return "redirect:/auth/login";

        try {
            var updated = foodQualityService.updateProduct(authUser.getId(), form.getId(), form);
            if (updated != null) {
                redirectAttributes.addFlashAttribute("success", "Data inspeksi diperbarui.");
                return "redirect:/food-products/" + form.getId(); // Sukses -> Detail Page
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/food-products/edit/" + form.getId(); // Error -> Balik ke form edit
    }

    // ==========================================================
    // 4. FITUR EDIT IMAGE (SEPARATE PAGE)
    // ==========================================================

    // [BARU] Menampilkan Halaman Ganti Gambar
    @GetMapping("/edit-image/{id}")
    public String showEditImagePage(@PathVariable UUID id, Model model) {
        if (getAuthUser() == null) return "redirect:/auth/login";

        ProductImageForm form = new ProductImageForm();
        form.setId(id);
        
        model.addAttribute("productImageForm", form);
        return "models/food-products/edit-cover"; // Mengarah ke edit-cover.html
    }

    @PostMapping("/edit-image")
    public String postEditProductImage(@Valid @ModelAttribute("productImageForm") ProductImageForm form,
            RedirectAttributes redirectAttributes) {

        User authUser = getAuthUser();
        if (authUser == null) return "redirect:/auth/login";

        if (form.isEmpty() || !form.isValidImage() || !form.isSizeValid(5 * 1024 * 1024)) {
            redirectAttributes.addFlashAttribute("error", "File tidak valid (Harus Gambar & Max 5MB)");
            return "redirect:/food-products/edit-image/" + form.getId();
        }

        try {
            if (foodQualityService.updateProductImage(authUser.getId(), form)) {
                redirectAttributes.addFlashAttribute("success", "Foto sampel diperbarui.");
                return "redirect:/food-products/" + form.getId();
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        
        return "redirect:/food-products/edit-image/" + form.getId();
    }

    // ==========================================================
    // 5. FITUR DELETE (SEPARATE PAGE)
    // ==========================================================

    // [BARU] Menampilkan Halaman Konfirmasi Hapus
    @GetMapping("/delete/{id}")
    public String showDeletePage(@PathVariable UUID id, Model model) {
        User authUser = getAuthUser();
        if (authUser == null) return "redirect:/auth/login";

        FoodProduct product = foodQualityService.getProductById(authUser.getId(), id);
        if (product == null) return "redirect:/food-products";

        model.addAttribute("foodProduct", product);
        return "models/food-products/delete"; // Mengarah ke delete.html
    }

    @PostMapping("/delete")
    public String postDeleteProduct(@RequestParam("id") UUID id, RedirectAttributes redirectAttributes) {
        User authUser = getAuthUser();
        if (authUser == null) return "redirect:/auth/login";

        try {
            boolean deleted = foodQualityService.deleteProduct(authUser.getId(), id);
            if (deleted) {
                redirectAttributes.addFlashAttribute("success", "Data produk berhasil dihapus.");
                return "redirect:/food-products";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Gagal hapus: " + e.getMessage());
        }
        
        return "redirect:/food-products/" + id;
    }

    // ==========================================================
    // 6. DETAIL PAGE
    // ==========================================================
    @GetMapping("/{id}")
    public String getDetailProduct(@PathVariable UUID id, Model model) {
        User authUser = getAuthUser();
        if (authUser == null) return "redirect:/auth/login";

        model.addAttribute("auth", authUser);
        
        FoodProduct product = foodQualityService.getProductById(authUser.getId(), id);
        if (product == null) return "redirect:/food-products";

        model.addAttribute("foodProduct", product);
        
        return "pages/food-products/detail";
    }

    // ==========================================================
    // 7. HELPER: SERVE IMAGE
    // ==========================================================
    @GetMapping("/image/{filename:.+}")
    @ResponseBody
    public Resource getImageByFilename(@PathVariable String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
        } catch (MalformedURLException e) {
            // ignore
        }
        return null;
    }

    private User getAuthUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof AnonymousAuthenticationToken) return null;
        return (auth.getPrincipal() instanceof User) ? (User) auth.getPrincipal() : null;
    }
        // [BARU] Menampilkan Halaman List Lengkap (list.html)
    @GetMapping("/list")
    public String showListPage(Model model, 
                               @RequestParam(required = false) String search,
                               @RequestParam(required = false) String status) {
        
        User authUser = getAuthUser();
        if (authUser == null) return "redirect:/auth/login";

        // Ambil semua produk
        List<FoodProduct> allProducts = foodQualityService.getAllProducts(authUser.getId());
        
        // Filter Logic (Search & Status)
        List<FoodProduct> displayList = allProducts.stream()
            .filter(p -> {
                // Filter by Search Keyword
                boolean matchSearch = true;
                if (search != null && !search.isBlank()) {
                    String k = search.toLowerCase();
                    matchSearch = p.getProductName().toLowerCase().contains(k) || 
                                  p.getBatchCode().toLowerCase().contains(k);
                }
                
                // Filter by Status (PASSED, REJECTED, PENDING)
                boolean matchStatus = true;
                if (status != null && !status.isBlank() && !status.equals("ALL")) {
                    matchStatus = p.getInspectionStatus().equalsIgnoreCase(status);
                }
                
                return matchSearch && matchStatus;
            })
            .collect(Collectors.toList());

        model.addAttribute("foodProducts", displayList);
        model.addAttribute("search", search); // Kirim balik keyword search ke UI
        
        // Return file template: templates/pages/food-products/list.html
        return "pages/food-products/list"; 
    }
    
}