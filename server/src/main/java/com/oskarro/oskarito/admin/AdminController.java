package com.oskarro.oskarito.admin;

import com.oskarro.oskarito.admin.AdminViews.*;
import org.springframework.web.bind.annotation.*;

/** Admin panel API. Access is limited to ROLE_ADMIN in WebSecurityConfig. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;

    public AdminController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/overview")
    Overview overview() {
        return admin.overview();
    }

    @GetMapping("/users")
    Page<UserRow> users(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "all") String filter,
                        @RequestParam(defaultValue = "new") String sort,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "25") int size) {
        return admin.users(q, filter, sort, page, size);
    }

    @GetMapping("/users/{id}")
    UserDetail user(@PathVariable Integer id) {
        return admin.user(id);
    }

    @PatchMapping("/users/{id}")
    UserDetail changeUser(@PathVariable Integer id, @RequestBody UserChange change) {
        return admin.changeUser(id, change);
    }

    @GetMapping("/tracks")
    Page<TrackRow> tracks(@RequestParam(defaultValue = "") String q,
                          @RequestParam(defaultValue = "") String genre,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "25") int size) {
        return admin.tracks(q, genre, page, size);
    }

    @PutMapping("/tracks/{id}")
    TrackRow changeTrack(@PathVariable Integer id, @RequestBody TrackChange change) {
        return admin.changeTrack(id, change);
    }

    @DeleteMapping("/tracks/{id}")
    void deleteTrack(@PathVariable Integer id) {
        admin.deleteTrack(id);
    }

    @GetMapping("/comments")
    Page<CommentRow> comments(@RequestParam(defaultValue = "") String q,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "25") int size) {
        return admin.comments(q, page, size);
    }

    @PutMapping("/comments/{id}")
    CommentRow changeComment(@PathVariable Integer id, @RequestBody CommentChange change) {
        return admin.changeComment(id, change);
    }

    @DeleteMapping("/comments/{id}")
    void deleteComment(@PathVariable Integer id) {
        admin.deleteComment(id);
    }

    @GetMapping("/activity")
    Page<Event> activity(@RequestParam(defaultValue = "all") String type,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "30") int size) {
        return admin.activity(type, page, size);
    }

    @GetMapping("/audit")
    Page<AuditRow> audit(@RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "30") int size) {
        return admin.auditLog(page, size);
    }
}
