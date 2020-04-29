package com.oskarro.oskarito.storage;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.stream.Stream;

@Service
public interface FilesStorageService {

    void init();

    void save(MultipartFile file, String username);

    Resource load(String filename, String username);

    void deleteAll();

    Stream<Path> loadAll();
}
