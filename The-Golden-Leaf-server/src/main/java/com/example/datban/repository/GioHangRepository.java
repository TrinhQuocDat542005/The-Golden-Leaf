package com.example.datban.repository;

import com.example.datban.model.GioHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GioHangRepository extends JpaRepository<GioHang, Long> {
        List<GioHang> findByIdDat(Long idDat); // Lấy tất cả món theo idDat

}
