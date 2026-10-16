package com.oskarro.oskarito.storage;

import com.oskarro.oskarito.track.CoverImages;
import com.oskarro.oskarito.user.User;
import com.oskarro.oskarito.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * Avatars live only in the database, re-encoded as a small JPEG, so nothing from the
 * request (file name, username) ever becomes a path on disk.
 */
@Service
public class FilesStorageServiceImpl implements FilesStorageService {

    private final UserRepository userRepository;
    private final ImageRepository imageRepository;

    public FilesStorageServiceImpl(UserRepository userRepository, ImageRepository imageRepository) {
        this.userRepository = userRepository;
        this.imageRepository = imageRepository;
    }

    @Override
    @Transactional
    public void saveAvatar(MultipartFile file, String username) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wybierz obrazek");
        }
        String type = file.getContentType() == null ? "" : file.getContentType();
        if (!type.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plik musi być obrazkiem");
        }
        byte[] jpeg;
        try {
            jpeg = CoverImages.miniaturize(file.getBytes());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nie udało się odczytać obrazka");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Zaloguj się"));
        Image image = imageRepository.findById(user.getId()).orElseGet(() -> Image.builder().user(user).build());
        image.setName("avatar.jpg");
        image.setType("image/jpeg");
        image.setPic(jpeg);
        imageRepository.save(image);
    }

    @Override
    @Transactional(readOnly = true)
    public Image loadAvatar(String username) {
        Image image = imageRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brak zdjęcia"));
        if (image.getPic() == null || image.getPic().length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Brak zdjęcia");
        }
        return image;
    }
}
