# 글로벌 엔티티 모듈

이 디렉토리는 Fream 백엔드 애플리케이션에서 공통으로 사용되는 기본 엔티티 클래스들을 포함합니다.

## BaseTimeEntity

모든 엔티티의 생성 시간과 수정 시간을 자동으로 관리하는 기본 클래스입니다.

```java
@MappedSuperclass
@Getter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {
    @Column(updatable = false)
    private LocalDateTime createdDate;  // 생성일 (최초 생성 시점)

    private LocalDateTime modifiedDate;  // 수정일 (마지막 수정 시점)

    @PrePersist
    protected void onCreate() {
        this.createdDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.modifiedDate = LocalDateTime.now();
    }
}
```

### 주요 기능

- **생성 시간 자동 기록**: 엔티티가 처음 저장될 때 자동으로 생성 시간을 기록합니다.
- **수정 시간 자동 기록**: 엔티티가 업데이트될 때 자동으로 수정 시간을 기록합니다.
- **@MappedSuperclass**: 이 클래스를 상속하는 엔티티에 필드를 포함시킵니다.
- **@EntityListeners**: Spring Data JPA의 Auditing 기능을 활성화합니다.

### 사용 예시

```java
// Product 엔티티가 BaseTimeEntity를 상속
public class Product extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private BigDecimal price;
    
    // 다른 필드와 메서드...
}

// 사용 예시 - 생성 시간과 수정 시간이 자동으로 기록됨
Product product = new Product();
product.setName("신상품");
product.setPrice(new BigDecimal("29900"));

productRepository.save(product);  // createdDate 자동 설정

// 이후에 업데이트 시
product.setPrice(new BigDecimal("19900"));
productRepository.save(product);  // modifiedDate 자동 설정
```

## BaseEntity

생성 시간, 수정 시간에 더해 생성자와 수정자 정보까지 자동으로 관리하는 확장 클래스입니다.

```java
@MappedSuperclass
@Getter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity extends BaseTimeEntity {
    @CreatedBy
    @Column(updatable = false)
    private String createdBy;  // 작성자 (최초 생성한 사용자)

    @LastModifiedBy
    private String modifiedBy;  // 수정자 (마지막 수정한 사용자)
}
```

### 주요 기능

- **BaseTimeEntity 상속**: 생성 시간과 수정 시간 관리 기능을 상속받습니다.
- **생성자 자동 기록**: `@CreatedBy` 애노테이션을 통해 엔티티 생성자를 자동으로 기록합니다.
- **수정자 자동 기록**: `@LastModifiedBy` 애노테이션을 통해 마지막 수정자를 자동으로 기록합니다.

### 사용 예시

```java
// Order 엔티티가 BaseEntity를 상속
public class Order extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private OrderStatus status;
    private BigDecimal totalAmount;
    
    // 다른 필드와 메서드...
}

// 사용 예시 - 생성자와 수정자 정보가 자동으로 기록됨
Order order = new Order();
order.setStatus(OrderStatus.PENDING);
order.setTotalAmount(new BigDecimal("99000"));

orderRepository.save(order);  // createdDate, createdBy 자동 설정

// 이후에 업데이트 시
order.setStatus(OrderStatus.SHIPPED);
orderRepository.save(order);  // modifiedDate, modifiedBy 자동 설정
```

## AuditorAwareImpl 설정

BaseEntity의 createdBy와 modifiedBy 필드에 현재 인증된 사용자 정보를 자동으로 주입하기 위해서는 다음과 같은 설정이 필요합니다:

```java
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            try {
                // SecurityUtils 클래스를 사용하여 현재 인증된 사용자의 이메일을 가져옴
                String email = SecurityUtils.extractEmailOrAnonymous();
                return Optional.of(email);
            } catch (Exception e) {
                return Optional.of("system");
            }
        };
    }
}
```

이 설정을 통해 엔티티가 저장되거나 업데이트될 때 현재 로그인한 사용자의 이메일이 createdBy와 modifiedBy 필드에 자동으로 기록됩니다.