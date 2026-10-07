package com.example.datban.service;

import com.example.datban.model.DatBan;

public interface DatBanService {
    DatBan getById(Long id, String actorEmail);
    DatBan getLatestDatBan(String email);
    DatBan getLatestDatBan();
}
