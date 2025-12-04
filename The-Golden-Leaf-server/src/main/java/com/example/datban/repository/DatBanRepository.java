package com.example.datban.repository;

import com.example.datban.model.DatBan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional; // <-- thêm dòng này

@Repository
public interface DatBanRepository extends JpaRepository<DatBan, Long> {
    DatBan findTopByEmailOrderByIdDatDesc(String email);
    Optional<DatBan> findTopByOrderByIdDatDesc(); // Lấy DatBan mới nhất
}
