package com.example.datban.service.impl;

import com.example.datban.exception.ResourceNotFoundException;
import com.example.datban.model.DatBan;
import com.example.datban.repository.DatBanRepository;
import com.example.datban.service.DatBanService;
import org.springframework.stereotype.Service;

@Service
public class DatBanServiceImpl implements DatBanService {

    private final DatBanRepository datBanRepository;

    public DatBanServiceImpl(DatBanRepository datBanRepository) {
        this.datBanRepository = datBanRepository;
    }

    @Override
    public DatBan getById(Long id, String actorEmail) {
        DatBan booking = datBanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đặt bàn"));
        com.example.datban.service.BookingLifecycleService.checkOwner(booking, actorEmail);
        return booking;
    }

    @Override
    public DatBan getLatestDatBan(String email) {
        var principal = com.example.datban.security.RestaurantPrincipal.current();
        if (principal != null) {
            var owned = datBanRepository.findTopByUserUidOrderByIdDatDesc(principal.uid());
            if (owned.isPresent()) return owned.get();
        }
        DatBan booking = datBanRepository.findTopByEmailOrderByIdDatDesc(email);
        if (booking == null) {
            throw new ResourceNotFoundException("Không tìm thấy lượt đặt bàn gần nhất");
        }
        com.example.datban.service.BookingLifecycleService.checkOwner(booking, email);
        return booking;
    }

    @Override
    public DatBan getLatestDatBan() {
        return datBanRepository.findTopByOrderByIdDatDesc()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt đặt bàn gần nhất"));
    }
}
