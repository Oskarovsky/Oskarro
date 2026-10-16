package com.oskarro.oskarito.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FilesStorageService {

    void saveAvatar(MultipartFile file, String username);

    Image loadAvatar(String username);
}
