package com.priceiq.service;

import com.priceiq.entity.Category;
import com.priceiq.entity.Product;
import com.priceiq.entity.ProductOffer;
import com.priceiq.entity.Store;
import com.priceiq.repository.CategoryRepository;
import com.priceiq.repository.ProductOfferRepository;
import com.priceiq.repository.ProductRepository;
import com.priceiq.repository.StoreRepository;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class ProductImportService {

    private static final Logger log = LoggerFactory.getLogger(ProductImportService.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final ProductOfferRepository offerRepository;

    public ProductImportService(ProductRepository productRepository,
                                CategoryRepository categoryRepository,
                                StoreRepository storeRepository,
                                ProductOfferRepository offerRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;
        this.offerRepository = offerRepository;
    }

    @Transactional
    public int importProductsFromExcel(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Yuklangan Excel fayli bo'sh!");
        }

        List<Product> productsToSave = new ArrayList<>();
        List<Long> parsedPrices = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            int rowNumber = 0;
            while (rows.hasNext()) {
                Row row = rows.next();

                // Skip Header Row (index 0)
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }
                rowNumber++;

                // Column 0: Title (String)
                Cell cell0 = row.getCell(0);
                String title = getCellValueAsString(cell0);
                if (title == null || title.trim().isEmpty()) {
                    continue; // Skip empty rows
                }

                // Column 1: Price (Long)
                Cell cell1 = row.getCell(1);
                String priceStr = getCellValueAsString(cell1);
                Long price = parseLongPrice(priceStr);

                // Column 2: Category (String)
                Cell cell2 = row.getCell(2);
                String catName = getCellValueAsString(cell2);
                Category category = resolveCategory(catName);

                // Column 3: Image URL (String)
                Cell cell3 = row.getCell(3);
                String imageUrl = getCellValueAsString(cell3);
                if (imageUrl == null || imageUrl.trim().isEmpty()) {
                    imageUrl = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80";
                }

                // Column 4: Rating (Double, default 5.0)
                Cell cell4 = row.getCell(4);
                Double rating = parseDoubleRating(getCellValueAsString(cell4));

                Product product = new Product();
                product.setTitleUz(title.trim());
                product.setTitleRu(title.trim());
                product.setTitleEn(title.trim());
                product.setImageUrl(imageUrl.trim());
                product.setCategory(category);
                product.setBrand("Imported");

                productsToSave.add(product);
                parsedPrices.add(price);
            }
        }

        if (productsToSave.isEmpty()) {
            return 0;
        }

        List<Product> savedProducts = productRepository.saveAll(productsToSave);
        Store defaultStore = resolveDefaultStore();

        for (int i = 0; i < savedProducts.size(); i++) {
            Product saved = savedProducts.get(i);
            Long price = parsedPrices.get(i);
            if (price != null && price > 0) {
                ProductOffer offer = new ProductOffer(null, saved, defaultStore, price, (long)(price * 1.1), true, "https://uzum.uz");
                offerRepository.save(offer);
            }
        }

        log.info("Successfully imported {} products from Excel file: {}", savedProducts.size(), file.getOriginalFilename());
        return savedProducts.size();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private Long parseLongPrice(String str) {
        if (str == null || str.trim().isEmpty()) return 0L;
        String clean = str.replaceAll("[^0-9]", "");
        if (clean.isEmpty()) return 0L;
        try {
            return Long.parseLong(clean);
        } catch (Exception e) {
            return 0L;
        }
    }

    private Double parseDoubleRating(String str) {
        if (str == null || str.trim().isEmpty()) return 5.0;
        try {
            double val = Double.parseDouble(str.trim());
            return (val >= 1.0 && val <= 5.0) ? val : 5.0;
        } catch (Exception e) {
            return 5.0;
        }
    }

    private Category resolveCategory(String name) {
        String catName = (name != null && !name.trim().isEmpty()) ? name.trim() : "Boshqa Mahsulotlar";
        return categoryRepository.findAll().stream()
                .filter(c -> c.getNameUz().equalsIgnoreCase(catName) || c.getNameRu().equalsIgnoreCase(catName))
                .findFirst()
                .orElseGet(() -> categoryRepository.save(new Category(null, catName, catName, catName, "Box")));
    }

    private Store resolveDefaultStore() {
        return storeRepository.findAll().stream().findFirst().orElseGet(() ->
                storeRepository.save(new Store(null, "Uzum Market", "https://uzum.uz", "https://uzum.uz", 4.8, "+998901234567", null))
        );
    }
}
