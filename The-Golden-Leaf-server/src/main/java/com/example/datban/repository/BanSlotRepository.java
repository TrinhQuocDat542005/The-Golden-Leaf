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

    @Query("select s.id from BanSlot s where s.ngay = :ngay and s.khungGio = :khungGio")
    Optional<Long> slotId(LocalDate ngay, String khungGio);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from BanSlot s where s.id = :id")
    Optional<BanSlot> lockSlotById(Long id);

    /** Resolve only a scalar first; serialize on one primary-key record, including reentrant locks.
     * Date/label are immutable. Avoid secondary-index lock contention under nested payment/invoice calls.
     * Caller must retain its transaction, as with every pessimistic repository lock.
     */
    default Optional<BanSlot> lockSlot(LocalDate ngay, String khungGio) {
        return slotId(ngay, khungGio).flatMap(this::lockSlotById)
                .filter(slot -> ngay.equals(slot.getNgay()) && khungGio.equals(slot.getKhungGio()));
    }
}
