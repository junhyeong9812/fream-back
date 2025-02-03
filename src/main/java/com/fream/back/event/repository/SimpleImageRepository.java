package com.fream.back.event.repository;

import com.fream.back.event.entity.SimpleImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SimpleImageRepository extends JpaRepository<SimpleImage, Long> {

    @Query("DELETE FROM SimpleImage s WHERE s.event.id = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);
    @Query("SELECT s FROM SimpleImage s WHERE s.event.id = :eventId")
    List<SimpleImage> findByEventId(@Param("eventId") Long eventId);
}
