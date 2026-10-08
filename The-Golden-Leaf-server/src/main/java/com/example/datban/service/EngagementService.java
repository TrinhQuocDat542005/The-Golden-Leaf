package com.example.datban.service;

import com.example.datban.dto.MenuItemResponse;
import com.example.datban.exception.ResourceNotFoundException;
import com.example.datban.repository.ThucDonRepository;
import com.example.datban.security.RestaurantPrincipal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Identity comes exclusively from the verified principal, never a client userId. */
@Service
@Transactional
public class EngagementService {
    private final JdbcTemplate db;
    private final ThucDonRepository menu;
    public EngagementService(JdbcTemplate db, ThucDonRepository menu) { this.db=db; this.menu=menu; }
    private String lockOwner() {
        String uid=RestaurantPrincipal.required().uid();
        // Serialize retries for this account without locking other customers' engagement writes.
        db.queryForList("SELECT uid FROM users WHERE uid=? FOR UPDATE",uid);
        return uid;
    }
    private void requireMenu(Long id) {
        if (!menu.findById(id).filter(item -> item.isActive()).isPresent())
            throw new ResourceNotFoundException("Món ăn không còn phục vụ");
    }
    @Transactional(readOnly=true)
    public List<MenuItemResponse> favorites() {
        String uid=RestaurantPrincipal.required().uid();
        return db.queryForList("SELECT f.menu_item_id FROM favorites f JOIN menu_items m ON m.id=f.menu_item_id WHERE f.user_uid=? AND m.active=TRUE ORDER BY f.created_at DESC,f.menu_item_id DESC LIMIT 100",Long.class,uid)
                .stream().map(id -> MenuItemResponse.from(menu.findById(id).orElseThrow())).toList();
    }
    public void favorite(Long id, boolean add) {
        String uid=lockOwner();
        if (!add) { db.update("DELETE FROM favorites WHERE user_uid=? AND menu_item_id=?",uid,id); return; }
        requireMenu(id);
        if (db.queryForObject("SELECT COUNT(*) FROM favorites WHERE user_uid=? AND menu_item_id=?",Integer.class,uid,id)==0)
            db.update("INSERT INTO favorites(user_uid,menu_item_id) VALUES(?,?)",uid,id);
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> reviews(Long id) {
        requireMenu(id);
        return db.queryForList("SELECT id,menu_item_id,rating,content,created_at FROM reviews WHERE menu_item_id=? AND status='PUBLISHED' ORDER BY updated_at DESC,id DESC LIMIT 100",id)
                .stream().map(this::reviewView).toList();
    }
    public Map<String,Object> review(Long id, String content, int rating) {
        String uid=lockOwner(); requireMenu(id);
        var existing=db.queryForList("SELECT id FROM reviews WHERE user_uid=? AND menu_item_id=? FOR UPDATE",Long.class,uid,id);
        if (existing.isEmpty()) db.update("INSERT INTO reviews(user_uid,menu_item_id,rating,content) VALUES(?,?,?,?)",uid,id,rating,content.strip());
        else db.update("UPDATE reviews SET rating=?,content=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",rating,content.strip(),existing.get(0));
        // Do not implicitly republish a review hidden by moderation.
        return reviewView(db.queryForList("SELECT id,menu_item_id,rating,content,created_at FROM reviews WHERE user_uid=? AND menu_item_id=?",uid,id).get(0));
    }
    private Map<String,Object> reviewView(Map<String,Object> row) {
        // Public feedback deliberately excludes UID/email/profile data.
        return Map.of("id",row.get("id"),"thucDon",Map.of("idThucDon",row.get("menu_item_id")),
                "authorName","Khách hàng","rating",row.get("rating"),"noiDung",row.get("content"),
                "createdAt",((Timestamp)row.get("created_at")).toInstant().toString());
    }
}
