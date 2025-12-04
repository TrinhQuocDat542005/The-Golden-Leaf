package com.example.datban.service;

import com.example.datban.model.ThucDon;
import com.example.datban.repository.ThucDonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ThucDonService {

    @Autowired
    private ThucDonRepository thucDonRepository;

    public List<ThucDon> getAllMonAn() {
        return thucDonRepository.findAll();
    }

    public void createMonAn(ThucDon monAn) {
        thucDonRepository.save(monAn);
    }
    public ThucDon getMonAnById(Long id) {
    return thucDonRepository.findById(id).orElse(null);
}

}
