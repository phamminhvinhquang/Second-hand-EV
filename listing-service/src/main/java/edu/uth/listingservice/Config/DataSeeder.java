package edu.uth.listingservice.Config;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.uth.listingservice.Model.ListingStatus;
import edu.uth.listingservice.Model.Product;
import edu.uth.listingservice.Model.ProductCondition;
import edu.uth.listingservice.Model.ProductImage;
import edu.uth.listingservice.Model.ProductListing;
import edu.uth.listingservice.Model.ProductSpecification;
import edu.uth.listingservice.Repository.ProductConditionRepository;
import edu.uth.listingservice.Repository.ProductImageRepository;
import edu.uth.listingservice.Repository.ProductListingRepository;
import edu.uth.listingservice.Repository.ProductRepository;
import edu.uth.listingservice.Repository.ProductSpecificationRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired private ProductRepository productRepo;
    @Autowired private ProductListingRepository listingRepo;
    @Autowired private ProductSpecificationRepository specRepo;
    @Autowired private ProductImageRepository imageRepo;
    @Autowired private ProductConditionRepository conditionRepo;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (listingRepo.count() == 0) {
            System.out.println("⚡ [Seeder] Đang tạo 15 tin đăng mẫu (User ID 3->7)...");

            // 1. Đảm bảo Condition tồn tại (QUAN TRỌNG)
            ProductCondition new99 = getOrCreateCondition(1L, "Mới 99% (Lướt)");
            ProductCondition good90 = getOrCreateCondition(2L, "Tốt 85-94% (Đã sử dụng)");
            ProductCondition okay80 = getOrCreateCondition(3L, "Khá 70-84% (Cần bảo dưỡng)");
            ProductCondition old70 = getOrCreateCondition(4L, "Trung bình dưới 70%");

            // ==========================================
            // USER 3: Chuyên bán xe VinFast lướt
            // ==========================================
            createListing( // 1. VF e34
                "VinFast VF e34 2022 - Pin Tiên Phong", "car", 480_000_000L, 490_000_000L,
                "Xe gia đình, cam kết không đâm đụng. Đã update phần mềm mới nhất.",
                // Spec: Brand, Year, Cond, ODO, Cap, Type, Life, Compat, Warranty
                "VinFast", 2022, new99, 12000L, "42 kWh", "Lithium-ion", null, null, "Còn bảo hành chính hãng",
                // Spec: Speed, Range, Color, ChargeTime, Cycles
                130, 285, "Xanh dương", "Fast Charge", 150L,
                Arrays.asList("/uploads/vf-e34.jpg"),
                3L, "Nguyễn Văn Một", "user3@example.com", "0903000001", "Quận 7, TP.HCM",
                ListingStatus.ACTIVE, true
            );

            createListing( // 2. VF 5 Plus
                "VF 5 Plus 2023 Màu Cam - Mới cứng", "car", 410_000_000L, 420_000_000L,
                "Xe trúng thưởng không dùng, mới chạy rodai. Giá tốt cho anh em chạy dịch vụ.",
                "VinFast", 2023, new99, 2000L, "37 kWh", "LFP", null, null, "Còn bảo hành chính hãng",
                130, 300, "Cam", "30 phút (10-70%)", 20L,
                Arrays.asList("/uploads/vf-5.jpg"),
                3L, "Nguyễn Văn Một", "user3@example.com", "0903000001", "Quận 7, TP.HCM",
                ListingStatus.ACTIVE, false
            );

            createListing( // 3. VinFast Evo 200
                "VinFast Evo 200 Lite - Chạy lướt", "motorbike", 19_000_000L, 19_500_000L,
                "Xe mua cho con đi học nhưng cháu đi du học nên bán. Pin thuê.",
                "VinFast", 2022, good90, 5000L, "LFP", "LFP", null, null, "Còn bảo hành chính hãng",
                70, 200, "Vàng", "4 giờ", 50L,
                Arrays.asList("/uploads/evo-200.jpg"),
                3L, "Nguyễn Văn Một", "user3@example.com", "0903000001", "Quận 7, TP.HCM",
                ListingStatus.ACTIVE, true
            );

            // ==========================================
            // USER 4: Fan của Dat Bike & Xe độ
            // ==========================================
            createListing( // 4. Weaver 200
                "Dat Bike Weaver 200 - Đã độ kiểng", "motorbike", 35_000_000L, 36_000_000L,
                "Xe hiệu suất cao, đã lên phuộc Ohlins, tay thắng Brembo. Pass lại lên đời.",
                "Dat Bike", 2021, good90, 8500L, "6 kWh", "Lithium-ion", null, null, "Hết bảo hành",
                90, 200, "Cam Đen", "3 giờ", 300L,
                Arrays.asList("/uploads/weaver-200.jpg"),
                4L, "Trần Văn Hai", "user4@example.com", "0904000002", "Cầu Giấy, Hà Nội",
                ListingStatus.ACTIVE, true
            );

            createListing( // 5. Weaver++
                "Dat Bike Weaver++ 2023 - Mới 99%", "motorbike", 55_000_000L, 58_000_000L,
                "Trải nghiệm xong cần bán. Xe mạnh, sạc siêu nhanh.",
                "Dat Bike", 2023, new99, 1500L, "7 kWh", "Lithium-ion", null, null, "Còn bảo hành chính hãng",
                100, 200, "Đen nhám", "20 phút (100km)", 15L,
                Arrays.asList("/uploads/weaver-plus.jpg"),
                4L, "Trần Văn Hai", "user4@example.com", "0904000002", "Cầu Giấy, Hà Nội",
                ListingStatus.ACTIVE, false
            );

            // --- PIN (Item 6): Loại bỏ các trường xe (ODO, Speed, Range, Color, ChargeTime, Cycles) ---
            createListing( // 6. Pin LFP thay thế
                "Pin LFP 30Ah khối nhôm", "battery", 3_800_000L, 4_000_000L,
                "Pin đóng mới nguyên khối, vỏ nhôm tản nhiệt tốt. Dòng xả cao.",
                // Spec: Brand, Year, Cond, ODO(0), Cap, Type, Life, Compat, Warranty
                "EVE", 2023, new99, 0L, "30Ah", "LFP", "9 tháng", "VinFast, Yadea", "BH 12 tháng",
                // Spec: Speed(0), Range(0), Color(null), ChargeTime(N/A), Cycles(0)
                0, 0, null, "N/A", 0L,
                Arrays.asList("/uploads/battery-lfp-9.jpg"), 
                4L, "Trần Văn Hai", "user4@example.com", "0904000002", "Cầu Giấy, Hà Nội",
                ListingStatus.ACTIVE, true
            );

            // ==========================================
            // USER 5: Chuyên xe máy điện phổ thông
            // ==========================================
            createListing( // 7. Klara S
                "VinFast Klara S 2020 - Pin LFP độ", "motorbike", 18_500_000L, 20_000_000L,
                "Xe nữ đi kỹ, đã thay pin LFP 22Ah đi xa 100km. Cốp rộng.",
                "VinFast", 2020, okay80, 25000L, "22Ah", "LFP", null, null, "Còn bảo hành chính hãng",
                50, 100, "Trắng", "6 giờ", 800L,
                Arrays.asList("/uploads/klara-s.jpg"),
                5L, "Lê Thị Ba", "user5@example.com", "0905000003", "Đà Nẵng",
                ListingStatus.ACTIVE, true
            );

            createListing( // 8. Feliz S
                "VinFast Feliz S 2022 - Chính chủ", "motorbike", 22_000_000L, 24_000_000L,
                "Xe đi làm hàng ngày, trầy xước nhẹ. Pin thuê chính hãng.",
                "VinFast", 2022, good90, 10000L, "LFP", "LFP", null, null, "Còn bảo hành chính hãng",
                78, 198, "Xám", "6 giờ", 200L,
                Arrays.asList("/uploads/feliz-s.jpg"),
                5L, "Lê Thị Ba", "user5@example.com", "0905000003", "Đà Nẵng",
                ListingStatus.ACTIVE, false
            );

            createListing( // 9. VF 9 (Hàng khủng)
                "VinFast VF 9 Plus - Ghế cơ trưởng", "car", 1_300_000_000L, 1_350_000_000L,
                "Xe chủ tịch, full option, trần kính. Mới đi 5000km.",
                "VinFast", 2023, new99, 5000L, "123 kWh", "NMC", null, null, "Hết bảo hành",
                200, 600, "Đen", "Fast Charge", 50L,
                Arrays.asList("/uploads/vf-9.jpg"),
                5L, "Lê Thị Ba", "user5@example.com", "0905000003", "Đà Nẵng",
                ListingStatus.ACTIVE, false 
            );

            // ==========================================
            // USER 6: Chuyên Pin & Linh kiện cũ
            // ==========================================
            createListing( // 10. Pin Pega
                "Thanh lý khối Pin Pega 30Ah", "battery", 4_500_000L, 5_000_000L,
                "Tháo xe Pega New Tech, dung lượng còn 85%. Thích hợp chế xe hoặc lưu trữ.",
                // PIN: ODO=0, Compat="Pega, Xe độ"
                "Pega", 2019, okay80, 0L, "30Ah", "Lithium-ion", "72 tháng", "Pega, Xe độ", "Còn bảo hành chính hãng",
                0, 0, null, "N/A", 0L,
                Arrays.asList("/uploads/battery-72v.jpg"),
                6L, "Phạm Văn Bốn", "user6@example.com", "0906000004", "Bình Dương",
                ListingStatus.ACTIVE, true
            );

            createListing( // 11. Pin LFP
                "Pin LFP 20Ah mới 100%", "battery", 3_200_000L, 3_500_000L,
                "Pin đóng mới, cell CATL, mạch JK Bluetooth. Bảo hành 1 năm.",
                // PIN: ODO=0, Compat="Xe máy điện 60V"
                "CATL", 2024, new99, 0L, "20Ah", "LFP", "3 tháng", "Xe máy điện 20Ah", "Còn bảo hành chính hãng",
                0, 0, null, "N/A", 0L,
                Arrays.asList("/uploads/battery-lfp.jpg"),
                6L, "Phạm Văn Bốn", "user6@example.com", "0906000004", "Bình Dương",
                ListingStatus.ACTIVE, true
            );

            createListing( // 12. Pega S (Xe)
                "Pega S 2021 - Xe ga điện sang chảnh", "motorbike", 25_000_000L, 27_000_000L,
                "Kiểu dáng SH, chạy êm, cốp rộng. Đã thay lốp Michelin.",
                "Pega", 2021, good90, 15000L, "32Ah", "Lithium", null, null, "Hết bảo hành",
                100, 120, "Đỏ", "8 giờ", 400L,
                Arrays.asList("/uploads/pega-s.jpg"),
                6L, "Phạm Văn Bốn", "user6@example.com", "0906000004", "Bình Dương",
                ListingStatus.ACTIVE, false
            );

            // ==========================================
            // USER 7: Xe Trung Quốc & Giá rẻ
            // ==========================================
            createListing( // 13. VF 8 Eco
                "VinFast VF 8 Eco - Màu xám", "car", 750_000_000L, 780_000_000L,
                "Xe công ty thanh lý, xuất hóa đơn. Bảo dưỡng định kỳ đầy đủ.",
                "VinFast", 2022, good90, 30000L, "82 kWh", "Lithium-ion", null, null, "Còn bảo hành chính hãng",
                200, 400, "Xám", "Fast Charge", 300L,
                Arrays.asList("/uploads/vf-8.jpg"),
                7L, "Hoàng Thị Năm", "user7@example.com", "0907000005", "Hải Phòng",
                ListingStatus.ACTIVE, true
            );

            createListing( // 14. Yadea G5
                "Yadea G5 - Thiết kế tương lai", "motorbike", 15_000_000L, 18_000_000L,
                "Xe đẹp lạ, màn hình lớn. Pin Panasonic tháo rời sạc tiện lợi.",
                "Yadea", 2020, okay80, 18000L, "24Ah", "Lithium Panasonic", null, null, "Hết bảo hành",
                50, 60, "Trắng", "6 giờ", 600L,
                Arrays.asList("/uploads/yadea-g5.jpg"),
                7L, "Hoàng Thị Năm", "user7@example.com", "0907000005", "Hải Phòng",
                ListingStatus.ACTIVE, false
            );

            createListing( // 15. Pin Lithium Cũ
                "Xả kho Pin 12Ah xe đạp điện", "battery", 800_000L, 1_000_000L,
                "Pin tháo xe đạp điện trợ lực, còn dùng tốt. Giá ve chai.",
                // PIN: ODO=0, Compat="Xe đạp điện"
                "Samsung", 2018, old70, 0L, "12Ah", "18650", null, "Xe đạp điện", "Còn bảo hành chính hãng",
                0, 0, null, "N/A", 0L,
                Arrays.asList("/uploads/battery-lfp.jpg"),
                7L, "Hoàng Thị Năm", "user7@example.com", "0907000005", "Hải Phòng",
                ListingStatus.ACTIVE, false
            );

            System.out.println("✅ [Seeder] Đã tạo xong 15 tin đăng mẫu!");
        }
    }

    // Hàm helper
    private void createListing(
            String name, String type, Long price, Long aiPrice, String desc,
            String brand, int year, ProductCondition cond, Long mileage,
            String batCap, String batType, String batLife, String compat, String warranty,
            int speed, int range, String color, String chargeTime, Long cycles,
            List<String> imageUrls,
            Long userId, String sellerName, String sellerEmail, String phone, String loc,
            ListingStatus status, boolean verified
    ) {
        Product p = new Product();
        p.setProductName(name);
        p.setProductType(type);
        p.setPrice(price);
        p.setAiSuggestedPrice(aiPrice);
        p.setDescription(desc);
        p = productRepo.save(p);

        ProductSpecification spec = new ProductSpecification();
        spec.setProduct(p);
        spec.setBrand(brand);
        spec.setYearOfManufacture(year);
        spec.setCondition(cond);
        spec.setMileage(mileage);
        spec.setBatteryCapacity(batCap);
        spec.setBatteryType(batType);
        spec.setBatteryLifespan(batLife);
        spec.setCompatibleVehicle(compat); // Chỉ dùng cho Pin
        spec.setWarrantyPolicy(warranty);
        spec.setMaxSpeed(speed);
        spec.setRangePerCharge(range);
        spec.setColor(color); // Null cho Pin
        spec.setChargeTime(chargeTime);
        spec.setChargeCycles(cycles);
        specRepo.save(spec);

        for (String url : imageUrls) {
            ProductImage img = new ProductImage();
            img.setProduct(p);
            img.setImageUrl(url);
            imageRepo.save(img);
        }

        ProductListing pl = new ProductListing();
        pl.setProduct(p);
        pl.setUserId(userId);
        pl.setSellerName(sellerName);
        pl.setSellerEmail(sellerEmail);
        pl.setPhone(phone);
        pl.setLocation(loc);
        pl.setListingStatus(status);
        pl.setVerified(verified);
        pl.setListingDate(new Date());
        pl.setUpdatedAt(new Date());
        listingRepo.save(pl);
    }

    private ProductCondition getOrCreateCondition(Long id, String name) {
        return conditionRepo.findById(id).orElseGet(() -> {
            ProductCondition c = new ProductCondition();
            c.setConditionId(id);
            c.setConditionName(name);
            return conditionRepo.save(c);
        });
    }
}