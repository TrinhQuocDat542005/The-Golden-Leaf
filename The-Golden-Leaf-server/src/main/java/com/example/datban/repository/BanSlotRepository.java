package com.example.datban.repository;

import com.example.datban.model.BanSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface BanSlotRepository extends JpaRepository<BanSlot, Long> {
    Optional<BanSlot> findByNgayAndKhungGio(LocalDate ngay, String khungGio);
}
