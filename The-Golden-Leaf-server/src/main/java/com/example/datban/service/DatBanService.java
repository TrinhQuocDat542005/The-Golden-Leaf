package com.example.datban.service;

import com.example.datban.model.DatBan;

public interface DatBanService {
    DatBan saveDatBan(DatBan datBan);          
    DatBan getLatestDatBan(String email);     
}
