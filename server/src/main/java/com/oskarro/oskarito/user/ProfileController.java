package com.oskarro.oskarito.user;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
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
    ProfileView update(@RequestBody Map<String, String> body) {
        return profileService.updateBio(body.get("bio"));
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
