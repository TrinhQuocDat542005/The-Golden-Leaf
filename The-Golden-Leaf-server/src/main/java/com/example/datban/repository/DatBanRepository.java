package com.example.datban.repository;

import com.example.datban.model.DatBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional; // <-- thêm dòng này
import java.util.List;
import java.time.Instant;
import java.time.LocalDate;
import com.example.datban.model.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface DatBanRepository extends JpaRepository<DatBan, Long> {
    Optional<DatBan> findByIdempotencyKey(String key);
    Optional<DatBan> findTopByUserUidOrderByIdDatDesc(String uid);

    interface SlotIdentity {
        LocalDate getNgay();
        String getKhungGio();
    }

    @Query("select b.ngay as ngay, b.khungGio as khungGio from DatBan b where b.idDat = :id")
    Optional<SlotIdentity> slotIdentity(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from DatBan b where b.idDat = :id")
    Optional<DatBan> lockBooking(Long id);

    @Query("select b.idDat from DatBan b where b.status = :status and b.holdExpiresAt <= :now order by b.idDat")
    List<Long> expiredIds(BookingStatus status, Instant now, org.springframework.data.domain.Pageable page);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from DatBan b where b.ngay = :ngay and b.khungGio = :khungGio "
            + "and b.status = :status and b.holdExpiresAt <= :now order by b.idDat")
    List<DatBan> lockExpiredInSlot(LocalDate ngay, String khungGio, BookingStatus status, Instant now);
    DatBan findTopByEmailOrderByIdDatDesc(String email);
    Optional<DatBan> findTopByOrderByIdDatDesc(); // Lấy DatBan mới nhất
}
