package com.oskarro.oskarito.storage;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping(value = "/api/storage")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class FilesController {

    private static final Set<String> SAFE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final FilesStorageService filesStorageService;

    public FilesController(FilesStorageService filesStorageService) {
        this.filesStorageService = filesStorageService;
    }

    /** Sets the avatar of the logged-in user; any "username" sent by older clients is ignored. */
    @PostMapping(value = "/upload")
    public ResponseMessage uploadFile(@RequestParam("file") MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        filesStorageService.saveAvatar(file, authentication.getName());
        return new ResponseMessage("Dodano zdjęcie. Odśwież stronę.");
    }

    @GetMapping(value = "/{username}/avatar")
    public ResponseEntity<byte[]> getImage(@PathVariable String username) {
        Image image = filesStorageService.loadAvatar(username);
        // Never echo arbitrary stored types (e.g. SVG could carry scripts on our origin).
        MediaType type = SAFE_TYPES.contains(image.getType())
                ? MediaType.parseMediaType(image.getType())
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(type)
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.getPic());
    }
}
