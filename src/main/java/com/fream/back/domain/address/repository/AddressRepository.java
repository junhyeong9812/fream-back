package com.fream.back.domain.address.repository;

import com.fream.back.domain.address.entity.Address;
import com.fream.back.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    /**
     * 특정 사용자의 모든 주소 조회
     */
    List<Address> findByUser(User user);

    /**
     * 특정 사용자의 특정 주소 조회
     */
    Optional<Address> findByIdAndUser(Long id, User user);

    /**
     * 특정 사용자의 기본 주소 조회
     */
    Optional<Address> findByUserAndIsDefaultTrue(User user);

    /**
     * 수신자 이름으로 주소 검색 (결정적 암호화 활용)
     * 암호화된 이름과 정확히 일치하는 주소들을 검색
     */
    @Query("SELECT a FROM Address a WHERE a.user = :user AND a.recipientName = :encryptedRecipientName")
    List<Address> findByUserAndRecipientName(@Param("user") User user,
                                             @Param("encryptedRecipientName") String encryptedRecipientName);

    /**
     * 전화번호로 주소 검색 (결정적 암호화 활용)
     * 암호화된 전화번호와 정확히 일치하는 주소들을 검색
     */
    @Query("SELECT a FROM Address a WHERE a.user = :user AND a.phoneNumber = :encryptedPhoneNumber")
    List<Address> findByUserAndPhoneNumber(@Param("user") User user,
                                           @Param("encryptedPhoneNumber") String encryptedPhoneNumber);

    /**
     * 주소로 검색 (결정적 암호화 활용)
     * 암호화된 주소와 정확히 일치하는 주소들을 검색
     */
    @Query("SELECT a FROM Address a WHERE a.user = :user AND a.address = :encryptedAddress")
    List<Address> findByUserAndAddress(@Param("user") User user,
                                       @Param("encryptedAddress") String encryptedAddress);

    /**
     * 우편번호로 주소 검색 (결정적 암호화 활용)
     * 암호화된 우편번호와 정확히 일치하는 주소들을 검색
     */
    @Query("SELECT a FROM Address a WHERE a.user = :user AND a.zipCode = :encryptedZipCode")
    List<Address> findByUserAndZipCode(@Param("user") User user,
                                       @Param("encryptedZipCode") String encryptedZipCode);

    /**
     * 복합 검색 - 여러 필드 조건으로 검색 (결정적 암호화 활용)
     * 모든 조건이 AND로 연결됨
     */
    @Query("SELECT a FROM Address a WHERE a.user = :user " +
            "AND (:encryptedRecipientName IS NULL OR a.recipientName = :encryptedRecipientName) " +
            "AND (:encryptedPhoneNumber IS NULL OR a.phoneNumber = :encryptedPhoneNumber) " +
            "AND (:encryptedAddress IS NULL OR a.address = :encryptedAddress) " +
            "AND (:encryptedZipCode IS NULL OR a.zipCode = :encryptedZipCode)")
    List<Address> findByUserAndMultipleFields(@Param("user") User user,
                                              @Param("encryptedRecipientName") String encryptedRecipientName,
                                              @Param("encryptedPhoneNumber") String encryptedPhoneNumber,
                                              @Param("encryptedAddress") String encryptedAddress,
                                              @Param("encryptedZipCode") String encryptedZipCode);

    /**
     * 특정 사용자의 주소 개수 조회
     */
    long countByUser(User user);

    /**
     * 특정 사용자의 기본 주소 여부 확인
     */
    boolean existsByUserAndIsDefaultTrue(User user);

    /**
     * 특정 사용자의 주소 중 기본 주소가 아닌 것들 조회
     */
    List<Address> findByUserAndIsDefaultFalse(User user);
}