package com.example.datban.service;

import com.example.datban.exception.ResourceNotFoundException;
import com.example.datban.model.ThucDon;
import com.example.datban.repository.ThucDonRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ThucDonService {

    private final ThucDonRepository thucDonRepository;

    public ThucDonService(ThucDonRepository thucDonRepository) {
        this.thucDonRepository = thucDonRepository;
    }

    public List<ThucDon> getAllMonAn() {
        return thucDonRepository.findAll();
    }

    public void createMonAn(ThucDon monAn) {
        thucDonRepository.save(monAn);
    }

    public ThucDon getMonAnById(Long id) {
        return thucDonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món ăn"));
    }
}
