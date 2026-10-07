package com.example.datban.service.impl;

import com.example.datban.exception.ResourceNotFoundException;
import com.example.datban.model.DatBan;
import com.example.datban.repository.DatBanRepository;
import com.example.datban.service.DatBanService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatBanServiceImpl implements DatBanService {

    private final DatBanRepository datBanRepository;

    public DatBanServiceImpl(DatBanRepository datBanRepository) {
        this.datBanRepository = datBanRepository;
    }

    @Override
    @Transactional
    public DatBan saveDatBan(DatBan datBan) {
        return datBanRepository.save(datBan);
    }

    @Override
    public DatBan getLatestDatBan(String email) {
        DatBan booking = datBanRepository.findTopByEmailOrderByIdDatDesc(email);
        if (booking == null) {
            throw new ResourceNotFoundException("Không tìm thấy lượt đặt bàn gần nhất");
        }
        return booking;
    }

    @Override
    public DatBan getLatestDatBan() {
        return datBanRepository.findTopByOrderByIdDatDesc()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đặt bàn gần nhất"));
    }
}
