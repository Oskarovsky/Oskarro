package com.oskarro.oskarito.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ProfileController {

    private final ProfileService profileService;
    private final AccountService accountService;

    public ProfileController(ProfileService profileService, AccountService accountService) {
        this.profileService = profileService;
        this.accountService = accountService;
    }

    /** RODO art. 20: everything we hold about the signed-in user, as JSON. */
    @GetMapping("/me/export")
    ResponseEntity<Map<String, Object>> export() {
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"oskarro-moje-dane.json\"")
                .body(accountService.export());
    }

    /** RODO art. 17: deletes the account and everything it created. Body: {"confirm": password or username}. */
    @DeleteMapping("/me")
    void deleteAccount(@RequestBody Map<String, String> body) {
        accountService.delete(body == null ? null : body.get("confirm"));
    }

    @GetMapping("/me")
    ProfileView me() {
        return profileService.me();
    }

    @GetMapping("/ranking")
    RankingView ranking() {
        return profileService.ranking();
    }

    @PutMapping("/me")
    ProfileView update(@RequestBody Map<String, Object> body) {
        return profileService.update(body == null ? Map.of() : body);
    }

    @PostMapping("/password")
    void changePassword(@RequestBody Map<String, String> body) {
        Map<String, String> note = body == null ? Map.of() : body;
        profileService.changePassword(note.get("current"), note.get("next"));
    }

    @GetMapping("/{username}")
    ProfileView view(@PathVariable String username) {
        return profileService.view(username);
    }

    @PutMapping("/{username}/rank")
    ProfileView grant(@PathVariable String username, @RequestBody Map<String, String> body) {
        return profileService.grantRank(username, body.get("title"));
    }
}
