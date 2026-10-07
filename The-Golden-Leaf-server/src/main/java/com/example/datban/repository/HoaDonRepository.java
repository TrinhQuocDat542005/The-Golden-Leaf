package com.example.datban.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.datban.model.HoaDon;

public interface HoaDonRepository extends JpaRepository<HoaDon, Long> {
    java.util.Optional<HoaDon> findByIdDat(Long idDat);
}
