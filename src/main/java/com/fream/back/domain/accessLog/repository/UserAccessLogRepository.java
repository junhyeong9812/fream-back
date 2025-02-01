package com.fream.back.domain.accessLog.repository;

import com.fream.back.domain.accessLog.entity.UserAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAccessLogRepository extends JpaRepository<UserAccessLog, Long>, UserAccessLogRepositoryCustom {
    boolean existsByEmailOrIpAddress(String email, String ipAddress);
    List<UserAccessLog> findByIsAnonymous(boolean isAnonymous);
}
