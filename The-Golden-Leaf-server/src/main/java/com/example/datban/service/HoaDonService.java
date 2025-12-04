package com.example.datban.service;

import com.example.datban.dto.HoaDonRequest;
import com.example.datban.model.HoaDon;
import com.example.datban.repository.HoaDonRepository;
import org.springframework.stereotype.Service;

@Service
public class HoaDonService {

    private final HoaDonRepository repo;

    public HoaDonService(HoaDonRepository repo) {
        this.repo = repo;
    }

    public HoaDon saveHoaDon(HoaDonRequest req) {

        HoaDon hd = new HoaDon();
        hd.setIdDat(req.idDat);
        hd.setTienBan(req.tienBan);
        hd.setTienAn(req.tienAn);
        hd.setTongTien(req.tongTien);

        return repo.save(hd);
    }
}
