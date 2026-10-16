package com.oskarro.oskarito.track;

import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class CoverStorage {

    private final Path directory = Paths.get("uploads", "covers");

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(directory);
    }

    public void write(Integer trackId, byte[] jpeg) {
        try {
            Files.write(directory.resolve(trackId + ".jpg"), jpeg);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nie udało się zapisać okładki");
        }
    }

    public byte[] read(Integer trackId) {
        Path path = directory.resolve(trackId + ".jpg");
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Brak okładki");
        }
        try {
            return Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nie udało się odczytać okładki");
        }
    }

    public boolean exists(Integer trackId) {
        return trackId != null && Files.isRegularFile(directory.resolve(trackId + ".jpg"));
    }
}
