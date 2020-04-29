package com.oskarro.oskarito.storage;

import com.oskarro.oskarito.user.User;
import com.oskarro.oskarito.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class FilesStorageServiceImpl implements FilesStorageService {

    @Autowired
    UserRepository userRepository;

    @Autowired
    ImageRepository imageRepository;

    private final Path rootPath = Paths.get("uploads");

    @Override
    public void init() {
        try {
            Files.createDirectory(rootPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }

    @Transactional
    @Override
    public void save(MultipartFile file, String username) {
        try {
            final Path userPath = Paths.get(rootPath.toString() + "/" + username);
            FileSystemUtils.deleteRecursively(userPath.toFile());
            Files.createDirectory(Paths.get(rootPath.toString() + "/" + username));
            Files.copy(file.getInputStream(), userPath.resolve(Objects.requireNonNull(file.getOriginalFilename())));


            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username or email:" + username));

            if (imageRepository.existsByUserId(user.getId())) {
                imageRepository.deleteByUserId(user.getId());
            }

            Image image = Image.builder()
                    .name(file.getOriginalFilename())
                    .user(user)
                    .type(file.getContentType())
                    .pic(file.getBytes())
                    .build();
            final Image savedImage = imageRepository.save(image);
            System.out.println("Image saved");

        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }

    @Override
    public Resource load(String filename, String username) {
        try {
            final Path userPath = Paths.get(rootPath.toString() + "/" + username);
            Path file = userPath.resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootPath.toFile());
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.rootPath, 1).filter(path -> !path.equals(this.rootPath)).map(this.rootPath::relativize);
        } catch (IOException e) {
            throw new RuntimeException("Could not load the files!");
        }
    }
}
