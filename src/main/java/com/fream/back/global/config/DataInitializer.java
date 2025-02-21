package com.fream.back.global.config;

import com.fream.back.domain.address.entity.Address;
import com.fream.back.domain.address.repository.AddressRepository;
import com.fream.back.domain.faq.entity.FAQ;
import com.fream.back.domain.faq.entity.FAQCategory;
import com.fream.back.domain.faq.repository.FAQRepository;
import com.fream.back.domain.inspection.entity.InspectionCategory;
import com.fream.back.domain.inspection.entity.InspectionStandard;
import com.fream.back.domain.inspection.repository.InspectionStandardRepository;
import com.fream.back.domain.notice.entity.Notice;
import com.fream.back.domain.notice.entity.NoticeCategory;
import com.fream.back.domain.notice.repository.NoticeRepository;
import com.fream.back.domain.notification.entity.Notification;
import com.fream.back.domain.notification.entity.NotificationCategory;
import com.fream.back.domain.notification.entity.NotificationType;
import com.fream.back.domain.notification.repository.NotificationRepository;
import com.fream.back.domain.order.entity.*;
import com.fream.back.domain.order.repository.OrderBidRepository;
import com.fream.back.domain.order.repository.OrderItemRepository;
import com.fream.back.domain.order.repository.OrderRepository;
import com.fream.back.domain.payment.dto.GeneralPaymentRequestDto;
import com.fream.back.domain.payment.entity.GeneralPayment;
import com.fream.back.domain.payment.entity.PaymentStatus;
import com.fream.back.domain.payment.repository.PaymentRepository;
import com.fream.back.domain.payment.service.command.PaymentCommandService;
import com.fream.back.domain.product.entity.*;
import com.fream.back.domain.product.entity.enumType.ColorType;
import com.fream.back.domain.product.entity.enumType.GenderType;
import com.fream.back.domain.product.entity.enumType.SizeType;
import com.fream.back.domain.product.repository.*;
import com.fream.back.domain.sale.entity.Sale;
import com.fream.back.domain.sale.entity.SaleBid;
import com.fream.back.domain.sale.entity.SaleStatus;
import com.fream.back.domain.sale.repository.SaleBidRepository;
import com.fream.back.domain.sale.repository.SaleRepository;
import com.fream.back.domain.shipment.entity.OrderShipment;
import com.fream.back.domain.shipment.entity.SellerShipment;
import com.fream.back.domain.shipment.entity.ShipmentStatus;
import com.fream.back.domain.shipment.repository.SellerShipmentRepository;
import com.fream.back.domain.shipment.service.command.OrderShipmentCommandService;
import com.fream.back.domain.style.entity.MediaUrl;
import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.entity.StyleOrderItem;
import com.fream.back.domain.style.repository.MediaUrlRepository;
import com.fream.back.domain.style.repository.StyleOrderItemRepository;
import com.fream.back.domain.style.repository.StyleRepository;
import com.fream.back.domain.user.entity.*;
import com.fream.back.domain.user.repository.BankAccountRepository;
import com.fream.back.domain.user.repository.ProfileRepository;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.domain.user.service.profile.ProfileCommandService;
import com.fream.back.domain.warehouseStorage.entity.WarehouseStorage;
import com.fream.back.domain.warehouseStorage.repository.WarehouseStorageRepository;
import com.fream.back.domain.warehouseStorage.service.command.WarehouseStorageCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
@org.springframework.core.annotation.Order(1)
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProfileCommandService profileCommandService;
    private final PasswordEncoder passwordEncoder;
    private final NoticeRepository noticeRepository;
    private final FAQRepository faqRepository;
    private final InspectionStandardRepository inspectionStandardRepository;
    private final NotificationRepository notificationRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductColorRepository productColorRepository;
    private final ProductSizeRepository productSizeRepository;
    private final ProductImageRepository productImageRepository;
    private final OrderBidRepository orderBidRepository;
    private final SaleBidRepository saleBidRepository;
    private final OrderRepository orderRepository;
    private final SaleRepository saleRepository;
    private final OrderItemRepository orderItemRepository;
    private final SellerShipmentRepository sellerShipmentRepository;
    private final WarehouseStorageRepository warehouseStorageRepository;
    private final PaymentCommandService paymentCommandService;
    private final OrderShipmentCommandService orderShipmentCommandService;
    private final WarehouseStorageCommandService warehouseStorageCommandService;
    private final PaymentRepository paymentRepository;
    private final AddressRepository addressRepository;
    private final BankAccountRepository bankAccountRepository;
    private final StyleRepository styleRepository;
    private final MediaUrlRepository mediaUrlRepository;
    private final StyleOrderItemRepository styleOrderItemRepository;
    private final ProfileRepository profileRepository;

//    @Transactional
    @Override
    public void run(String... args) {
        // 사용자 생성
        User user1 = createUserWithProfile("user1@example.com", "password123!", "010-1234-5678", ShoeSize.SIZE_270, Role.USER, 25, Gender.MALE);
        User user2 = createUserWithProfile("user2@example.com", "password456!", "010-9876-5432", ShoeSize.SIZE_280, Role.USER, 30, Gender.FEMALE);
        User admin = createUserWithProfile("admin@example.com", "adminpassword!", "010-0000-0000", null, Role.ADMIN, 35, Gender.MALE);

        // 사용자 기본 주소 생성
        createAddress(user1, "홍길동", "010-1234-5678", "12345", "서울시 강남구 도산대로", "아파트 101호", true);
        createAddress(user2, "김철수", "010-9876-5432", "67890", "서울시 강서구 화곡로", "빌라 202호", true);
        createAddress(admin, "관리자", "010-0000-0000", "54321", "서울시 종로구 종로", "사무실 303호", true);

        // 사용자마다 은행 계좌 생성
        createBankAccount(user1, "국민은행", "123-4567-8901", "홍길동");
        createBankAccount(user2, "신한은행", "987-6543-2101", "김철수");
        createBankAccount(admin, "우리은행", "456-7890-1234", "관리자");

        // 상품 데이터 생성
        createProductData();
        createNoticeData();
        createFAQData();
        createInspectionStandardData();
        createNotificationData(user1, user2, admin);
        createStyleData(user1, user2);
        List<ProductSize> productSizes = productSizeRepository.findAll();

        // Create OrderBid and SaleBid for user1
        for (int i = 0; i < 5; i++) {
            ProductSize productSize = productSizes.get(i % productSizes.size());
            createOrderBid(user1, productSize, 5000 + i * 1000);
            createSaleBid(user1, productSize, 8000 + i * 1000, "123 Street", "12345", "010-1111-2222", false);
        }

        // Create OrderBid and SaleBid for user2
        for (int i = 0; i < 5; i++) {
            ProductSize productSize = productSizes.get((i + 5) % productSizes.size());
            createOrderBid(user2, productSize, 6000 + i * 1000);
            createSaleBid(user2, productSize, 9000 + i * 1000, "456 Avenue", "67890", "010-3333-4444", true);
        }

        // Link user1's 3 OrderBids to Sales from user2
        for (int i = 0; i < 3; i++) {
            OrderBid orderBid = orderBidRepository.findAll().get(i);
            ProductSize productSize = orderBid.getProductSize();
            Sale sale = createSale(user2, productSize, "Seller's Return Address", "54321", "010-5555-6666");
            linkOrderBidToSale(orderBid, sale);

            createSellerShipment(sale, "Fast Courier", "TRACK1234" + i);
        }

        // User2 purchasing from User1's Sales
        for (int i = 0; i < 3; i++) {
            SaleBid saleBid = saleBidRepository.findAll().get(i);
            processPurchaseAndShipment(user2, saleBid);
        }

        System.out.println("초기 데이터가 성공적으로 생성되었습니다.");
    }

    private User createUserWithProfile(String email, String password, String phoneNumber, ShoeSize shoeSize, Role role, Integer age, Gender gender) {
        if (userRepository.existsByEmail(email)) {
            return userRepository.findByEmail(email).orElseThrow();
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .phoneNumber(phoneNumber)
                .referralCode(generateReferralCode())
                .shoeSize(shoeSize)
                .termsAgreement(true)
                .phoneNotificationConsent(true)
                .emailNotificationConsent(true)
                .optionalPrivacyAgreement(true)
                .role(role)
                .age(age)
                .gender(gender)
                .build();

        User savedUser = userRepository.save(user);
        profileCommandService.createDefaultProfile(savedUser);
        return savedUser;
    }

    private void createNoticeData() {
        for (NoticeCategory category : NoticeCategory.values()) {
            for (int i = 1; i <= 10; i++) {
                noticeRepository.save(
                        Notice.builder()
                                .title(category + " 공지사항 제목 " + i)
                                .content("<h1>" + category + " 공지사항 내용 " + i + "</h1>")
                                .category(category)
                                .build()
                );
            }
        }
    }

    private void createFAQData() {
        for (FAQCategory category : FAQCategory.values()) {
            for (int i = 1; i <= 10; i++) {
                faqRepository.save(
                        FAQ.builder()
                                .question(category + " 질문 " + i)
                                .answer("<p>" + category + " 답변 내용 " + i + "</p>")
                                .category(category)
                                .build()
                );
            }
        }
    }

    private void createInspectionStandardData() {
        for (InspectionCategory category : InspectionCategory.values()) {
            inspectionStandardRepository.save(
                    InspectionStandard.builder()
                            .category(category)
                            .content("<ul><li>" + category + " 검수 기준 내용</li></ul>")
                            .build()
            );
        }
    }

    private void createNotificationData(User... users) {
        for (User user : users) {
            for (NotificationType type : NotificationType.values()) {
                if (type.getCategory() == NotificationCategory.SHOPPING) {
                    notificationRepository.save(
                            Notification.builder()
                                    .user(user)
                                    .category(type.getCategory())
                                    .type(type)
                                    .message(user.getEmail() + "의 쇼핑 알림: " + type.name())
                                    .isRead(false)
                                    .build()
                    );
                }
            }
        }
    }


    private String generateReferralCode() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void createProductData() {
        // 브랜드 생성
        Brand nike = brandRepository.save(Brand.builder().name("Nike").build());
        Brand newBalance = brandRepository.save(Brand.builder().name("New Balance").build());
        Brand adidas = brandRepository.save(Brand.builder().name("Adidas").build());
        Brand jordan = brandRepository.save(Brand.builder().name("Jordan").build());
        Brand stussy = brandRepository.save(Brand.builder().name("Stussy").build());
        Brand iabStudio = brandRepository.save(Brand.builder().name("IAB Studio").build());
        Brand newJeans = brandRepository.save(Brand.builder().name("NewJeans").build());

        // 카테고리 생성
        Category clothing = categoryRepository.save(Category.builder().name("Clothing").build());
        Category tops = categoryRepository.save(Category.builder().name("Tops").parentCategory(clothing).build());
        Category tshirts = categoryRepository.save(Category.builder().name("Short Sleeve T-Shirts").parentCategory(tops).build());

        Category shoes = categoryRepository.save(Category.builder().name("Shoes").build());
        Category sneakers = categoryRepository.save(Category.builder().name("Sneakers").parentCategory(shoes).build());

        // 상품 생성
//        createProductsForCategory("Sneakers", sneakers, List.of(nike, adidas, newBalance), SizeType.SHOES);
//        createProductsForCategory("Short Sleeve T-Shirts", tshirts, List.of(stussy, iabStudio, newJeans), SizeType.CLOTHING);
        List<List<ColorType>> predefinedColors = List.of(
                List.of(ColorType.BLACK, ColorType.GREY, ColorType.NAVY),
                List.of(ColorType.WHITE, ColorType.ORANGE, ColorType.MINT),
                List.of(ColorType.PINK, ColorType.GOLD, ColorType.MINT),
                List.of(ColorType.WHITE, ColorType.BLACK, ColorType.ORANGE),
                List.of(ColorType.GREEN, ColorType.BLACK, ColorType.GREY),
                List.of(ColorType.WHITE, ColorType.BLACK, ColorType.SKY_BLUE),
                List.of(ColorType.BLACK, ColorType.IVORY, ColorType.WHITE),
                List.of(ColorType.BLACK, ColorType.GREY, ColorType.BROWN),
                List.of(ColorType.ORANGE, ColorType.PURPLE, ColorType.RED),
                List.of(ColorType.ORANGE, ColorType.RED, ColorType.KHAKI)
        );

        List<ColorType> singleColors = List.of(
                ColorType.BLACK, ColorType.WHITE, ColorType.PINK, ColorType.WHITE, ColorType.WHITE,
                ColorType.PURPLE, ColorType.RED, ColorType.KHAKI, ColorType.NAVY, ColorType.GREY
        );

        for (int i = 1; i <= 20; i++) {
            String categoryName;
            Category category;
            List<Brand> brands;
            SizeType sizeType;

            if (i <= 10) {
                // 1~10번은 Sneakers
                categoryName = "Sneakers";
                category = sneakers;
                brands = List.of(nike, adidas, newBalance);
                sizeType = SizeType.SHOES;
            } else {
                // 11~20번은 Short Sleeve T-Shirts
                categoryName = "Short Sleeve T-Shirts";
                category = tshirts;
                brands = List.of(stussy, iabStudio, newJeans);
                sizeType = SizeType.CLOTHING;
            }

            Product product = productRepository.save(
                    Product.builder()
                            .name(categoryName + " Product " + i)
                            .englishName(categoryName + " English Product " + i)
                            .releasePrice(100 + i * 50)
                            .modelNumber("Model-" + i)
                            .releaseDate("2023-01-" + (i < 10 ? "0" + i : i))
                            .gender(GenderType.values()[new Random().nextInt(GenderType.values().length)])
                            .brand(brands.get((i - 1) % brands.size())) // 상품 번호 기반으로 브랜드 선택
                            .category(category)
                            .build()
            );

            if (i <= 10) {
                // 1번부터 10번까지는 predefinedColors 사용
                List<ColorType> colorsForProduct = predefinedColors.get(i - 1);

                for (ColorType color : colorsForProduct) {
                    ProductColor productColor = productColorRepository.save(
                            ProductColor.builder()
                                    .colorName(color.getDisplayName())
                                    .product(product)
                                    .build()
                    );

                    String imageName = "thumbnail_" + product.getId() + "_" + color.name().toUpperCase() + ".jpg";
                    ProductImage thumbnail = productImageRepository.save(
                            ProductImage.builder()
                                    .imageUrl("https://www.pinjun.xyz/api/products/query/" + product.getId() + "/images?imageName=" + imageName)
                                    .productColor(productColor)
                                    .build()
                    );

                    productColor.addThumbnailImage(thumbnail);
                    productColorRepository.save(productColor);

                    for (String size : sizeType.getSizes()) {
                        productSizeRepository.save(
                                ProductSize.builder()
                                        .size(size)
                                        .sizeType(sizeType)
                                        .purchasePrice(product.getReleasePrice())
                                        .salePrice(product.getReleasePrice() + 20)
                                        .quantity(10)
                                        .productColor(productColor)
                                        .build()
                        );
                    }
                }
            } else {
                // 11번부터 20번까지는 단일 색상 사용
                ColorType color = singleColors.get(i - 11);

                ProductColor productColor = productColorRepository.save(
                        ProductColor.builder()
                                .colorName(color.getDisplayName())
                                .product(product)
                                .build()
                );

                String imageName = "thumbnail_" + product.getId() + "_" + color.name().toLowerCase() + ".jpg";
                productColor.addThumbnailImage(
                        productImageRepository.save(
                                ProductImage.builder()
                                        .imageUrl("/api/products/" + product.getId() + "/images?imageName=" + imageName)
                                        .productColor(productColor)
                                        .build()
                        )
                );

                for (String size : sizeType.getSizes()) {
                    productSizeRepository.save(
                            ProductSize.builder()
                                    .size(size)
                                    .sizeType(sizeType)
                                    .purchasePrice(product.getReleasePrice())
                                    .salePrice(product.getReleasePrice() + 20)
                                    .quantity(10)
                                    .productColor(productColor)
                                    .build()
                    );
                }
            }
        }
    }
    private void createOrderBid(User user, ProductSize productSize, int bidPrice) {
        Order order = createOrder(user, productSize, bidPrice);
        OrderBid orderBid = OrderBid.builder()
                .user(user)
                .productSize(productSize)
                .bidPrice(bidPrice)
                .status(BidStatus.PENDING)
                .order(order)
                .build();
        orderBidRepository.save(orderBid);
    }

    private Order createOrder(User user, ProductSize productSize, int bidPrice) {
        OrderItem orderItem = OrderItem.builder()
                .productSize(productSize)
                .quantity(1)
                .price(bidPrice)
                .build();

        Order order = Order.builder()
                .user(user)
                .totalAmount(bidPrice)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        order.addOrderItem(orderItem);
        return orderRepository.save(order);
    }

    private void createSaleBid(User user, ProductSize productSize, int bidPrice, String returnAddress, String postalCode, String receiverPhone, boolean isWarehouseStorage) {
        Sale sale = Sale.builder()
                .seller(user)
                .productSize(productSize)
                .returnAddress(returnAddress)
                .postalCode(postalCode)
                .receiverPhone(receiverPhone)
                .isWarehouseStorage(isWarehouseStorage)
                .status(SaleStatus.PENDING_SHIPMENT)
                .build();

        saleRepository.save(sale);

        SaleBid saleBid = SaleBid.builder()
                .seller(user)
                .productSize(productSize)
                .bidPrice(bidPrice)
                .status(com.fream.back.domain.sale.entity.BidStatus.PENDING)
                .sale(sale)
                .build();

        saleBidRepository.save(saleBid);
    }

    private Sale createSale(User seller, ProductSize productSize, String returnAddress, String postalCode, String receiverPhone) {
        Sale sale = Sale.builder()
                .seller(seller)
                .productSize(productSize)
                .returnAddress(returnAddress)
                .postalCode(postalCode)
                .receiverPhone(receiverPhone)
                .status(SaleStatus.PENDING_SHIPMENT)
                .build();

        return saleRepository.save(sale);
    }

    private void linkOrderBidToSale(OrderBid orderBid, Sale sale) {
        orderBid.assignSale(sale);
        orderBid.updateStatus(BidStatus.MATCHED);
        orderBidRepository.save(orderBid);
    }

    private void createSellerShipment(Sale sale, String courier, String trackingNumber) {
        SellerShipment shipment = SellerShipment.builder()
                .sale(sale)
                .courier(courier)
                .trackingNumber(trackingNumber)
                .status(ShipmentStatus.IN_TRANSIT)
                .build();

        sellerShipmentRepository.save(shipment);
    }

    private void processPurchaseAndShipment(User user, SaleBid saleBid) {
        // Create Order
        Order order = Order.builder()
                .user(user)
                .totalAmount(saleBid.getBidPrice())
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        order = orderRepository.save(order);

        // 2. Create OrderBid and associate with Order
        OrderBid orderBid = OrderBid.builder()
                .user(user)
                .productSize(saleBid.getProductSize())
                .bidPrice(saleBid.getBidPrice())
                .status(BidStatus.MATCHED) // 즉시 매칭된 상태
                .order(order) // Order와 연관 설정
                .build();

        order.assignOrderBid(orderBid); // Order와 OrderBid 양방향 연관 설정
        orderBidRepository.save(orderBid);

        // Process Payment
        GeneralPaymentRequestDto paymentRequest = new GeneralPaymentRequestDto();
        paymentRequest.setPaidAmount(saleBid.getBidPrice());
        paymentRequest.setPaymentType("GENERAL");
        paymentRequest.setBuyerEmail(user.getEmail());
        paymentRequest.setBuyerName(user.getEmail()); // Assuming name for simplicity

        GeneralPayment generalPayment = GeneralPayment.builder()
                .impUid(UUID.randomUUID().toString()) // 더미 값
                .pgProvider("DummyProvider")        // 더미 값
                .receiptUrl("http://dummy-receipt-url.com") // 더미 URL
                .buyerName(user.getEmail())
                .buyerEmail(user.getEmail())
                .paidAmount(saleBid.getBidPrice())
                .build();

        generalPayment.assignOrder(order);
        generalPayment.assignUser(user);
        generalPayment.updateSuccessStatus(true);
        generalPayment.updateStatus(PaymentStatus.PAID);
        paymentRepository.save(generalPayment);

        order.assignPayment(generalPayment);
        // Update Statuses Sequentially
        order.updateStatus(OrderStatus.PAYMENT_COMPLETED);
        order.updateStatus(OrderStatus.PREPARING);



        // Create Shipment
        OrderShipment shipment = orderShipmentCommandService.createOrderShipment(order, "Receiver Name", "010-1111-2222", "54321", "Default Address");
        order.assignOrderShipment(shipment);

        // Update Warehouse Storage if applicable
        WarehouseStorage warehouseStorage = warehouseStorageCommandService.createOrderStorage(order, user);
        order.assignWarehouseStorage(warehouseStorage);
        order.updateStatus(OrderStatus.IN_WAREHOUSE);
        order.updateStatus(OrderStatus.COMPLETED);

        saleBid.assignOrder(order);
        saleBid.updateStatus(com.fream.back.domain.sale.entity.BidStatus.MATCHED);
        saleBidRepository.save(saleBid);
        orderRepository.save(order);
    }
    // 주소 생성 메서드
    private void createAddress(User user, String recipientName, String phoneNumber, String zipCode, String address, String detailedAddress, boolean isDefault) {
        Address newAddress = Address.builder()
                .user(user) // 연관관계 설정
                .recipientName(recipientName)
                .phoneNumber(phoneNumber)
                .zipCode(zipCode)
                .address(address)
                .detailedAddress(detailedAddress)
                .isDefault(isDefault)
                .build();

        addressRepository.save(newAddress);
        user.addAddress(newAddress); // 편의 메서드를 통해 User와 Address 연관 설정
    }
    // 은행 계좌 생성 메서드
    private void createBankAccount(User user, String bankName, String accountNumber, String accountHolder) {
        BankAccount bankAccount = BankAccount.builder()
                .user(user) // 사용자와 연관 설정
                .bankName(bankName)
                .accountNumber(accountNumber)
                .accountHolder(accountHolder)
                .build();

        bankAccountRepository.save(bankAccount);
    }
//    private void createStyleData(User user1, User user2) {
//        // 구매 완료되고 창고 보관 중인 주문의 OrderItem만 필터링
////        List<OrderItem> user1CompletedOrderItems = orderItemRepository.findAll().stream()
////                .filter(item -> item.getOrder().getUser().equals(user1)
////                        && item.getOrder().getStatus() == OrderStatus.COMPLETED)
//////                        && item.getOrder().getWarehouseStorage() != null)
////                .limit(1)  // 1개만 가져오기
////                .toList();
//
//        List<OrderItem> user2CompletedOrderItems = orderItemRepository.findAll().stream()
//                .filter(item -> item.getOrder().getUser().equals(user2)
//                        && item.getOrder().getStatus() == OrderStatus.COMPLETED)
////                        && item.getOrder().getWarehouseStorage() != null)
//                .limit(2)  // 1개만 가져오기
//                .toList();
//
////        if (!user1CompletedOrderItems.isEmpty()) {
////            createStylesForUser(user1, user1CompletedOrderItems.get(0), 1);
////        }
//        if (!user2CompletedOrderItems.isEmpty()) {
//            createStylesForUser(user2, user2CompletedOrderItems.get(0), 0);
//            createStylesForUser(user2, user2CompletedOrderItems.get(1), 20);  // user1 다음 번호부터 시작
//        }
//    }
//
//    private void createStylesForUser(User user, OrderItem orderItem, int startIndex) {
//        Long profileId = user.getProfile().getId();  // Profile ID만 꺼냄
//        Long productId = orderItem.getProductSize().getProductColor().getProduct().getId();  // Product ID만 꺼냄
//
//        // 리포지토리를 통해 다시 조회 (영속 상태 유지)
//        Profile profile = profileRepository.findById(profileId)
//                .orElseThrow(() -> new IllegalArgumentException("Profile not found with id: " + profileId));
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));
//
//        String productName = product.getName();
//
//        // 20개의 스타일 생성
//        for (int i = 0; i < 20; i++) {
//            Style style = Style.builder()
//                    .profile(profile)
//                    .content("안녕하세요! " + productName + "와 함께한 " + (i + 1) + "번째 스타일을 공유합니다 " +
//                            "#데일리룩 #fashion #ootd #" + product.getBrand().getName())
//                    .viewCount(0L)
//                    .build();
//
//            style.assignProfile(profile);
//
//            // 각 스타일마다 3개의 이미지
//            for (int j = 1; j <= 3; j++) {
//                MediaUrl mediaUrl = MediaUrl.builder()
//                        .url("media_" + (startIndex + i) + "_" + j + ".jpg")
//                        .build();
//                style.addMediaUrl(mediaUrl);
//            }
//
//            // 동일한 OrderItem을 모든 스타일에 연결
//            StyleOrderItem styleOrderItem = StyleOrderItem.builder()
//                    .orderItem(orderItem)
//                    .build();
//            style.addStyleOrderItem(styleOrderItem);
//
//            styleRepository.save(style);
//        }
//    }
private void createStyleData(User user1, User user2) {
    // 명시적으로 영속성 컨텍스트에서 필요한 데이터를 먼저 로드
    List<Order> completedOrders = orderRepository.findByUserAndStatus(user2, OrderStatus.COMPLETED);
    if (completedOrders.isEmpty()) {
        return;
    }

    // Fetch all necessary data eagerly
    List<OrderItem> completedOrderItems = orderItemRepository.findByOrderIn(completedOrders);
    if (completedOrderItems.size() >= 2) {
        OrderItem firstItem = completedOrderItems.get(0);
        OrderItem secondItem = completedOrderItems.get(1);

        // Explicitly load all required relationships
        Product firstProduct = productRepository.findById(
                firstItem.getProductSize().getProductColor().getProduct().getId()).orElseThrow();
        Product secondProduct = productRepository.findById(
                secondItem.getProductSize().getProductColor().getProduct().getId()).orElseThrow();

        Profile profile = profileRepository.findByUser(user2).orElseThrow();

        // Create styles in separate transactions
        createStylesForUserWithTransaction(profile, firstItem, firstProduct, 0);
        createStylesForUserWithTransaction(profile, secondItem, secondProduct, 20);
    }
}

    @Transactional
    public void createStylesForUserWithTransaction(Profile profile, OrderItem orderItem, Product product, int startIndex) {
        String productName = product.getName();

        for (int i = 0; i < 20; i++) {
            // 1. Create and save Style first
            Style style = Style.builder()
                    .profile(profile)
                    .content("안녕하세요! " + productName + "와 함께한 " + (i + 1) +
                            "번째 스타일을 공유합니다 #데일리룩 #fashion #ootd #" +
                            product.getBrand().getName())
                    .viewCount(0L)
                    .build();

            style = styleRepository.save(style);  // Save immediately to get ID

            // 2. Create and associate MediaUrls
            for (int j = 1; j <= 3; j++) {
                MediaUrl mediaUrl = MediaUrl.builder()
                        .url("media_" + (startIndex + i) + "_" + j + ".jpg")
                        .build();
                mediaUrl.assignStyle(style);  // Set relationship before save
                mediaUrlRepository.save(mediaUrl);
            }

            // 3. Create and associate StyleOrderItem
            StyleOrderItem styleOrderItem = StyleOrderItem.builder()
                    .orderItem(orderItem)
                    .build();
            styleOrderItem.assignStyle(style);  // Set relationship before save
            styleOrderItemRepository.save(styleOrderItem);

            // 4. Set bi-directional relationship with Profile
            style.assignProfile(profile);

            // 5. Final save to ensure all relationships are persisted
            styleRepository.save(style);

            // Add flush to ensure each style is completely persisted
            styleRepository.flush();
        }
    }

}




