package com.example.datban.repository;

import com.example.datban.model.BanSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.Optional;

public interface BanSlotRepository extends JpaRepository<BanSlot, Long> {
    java.util.List<BanSlot> findByNgayBetweenOrderByNgayAscKhungGioAsc(LocalDate start, LocalDate end);
    Optional<BanSlot> findByNgayAndKhungGio(LocalDate ngay, String khungGio);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from BanSlot s where s.ngay = :ngay and s.khungGio = :khungGio")
    Optional<BanSlot> lockSlot(LocalDate ngay, String khungGio);
}
