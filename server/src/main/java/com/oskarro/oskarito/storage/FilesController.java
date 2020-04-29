package com.oskarro.oskarito.storage;

import com.oskarro.oskarito.user.UserRepository;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import javax.servlet.ServletContext;
import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = "/api/storage")
@CrossOrigin(origins = "http://localhost:4200")
public class FilesController {

    @Autowired
    ServletContext servletContext;

    com.oskarro.oskarito.storage.FilesStorageService filesStorageService;
    ImageRepository imageRepository;
    UserRepository userRepository;

    public FilesController(com.oskarro.oskarito.storage.FilesStorageService filesStorageService, ImageRepository imageRepository, UserRepository userRepository) {
        this.filesStorageService = filesStorageService;
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/upload")
    public ResponseEntity<ResponseMessage> uploadFile(@RequestParam("file") MultipartFile file,
                                                      @RequestParam("username") String username) {
        String message = "";
        try {
            filesStorageService.save(file, username);
            message = "Dodano zdjęcie: " + file.getOriginalFilename() + ". Odśwież stronę.";
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
        } catch (Exception e) {
            message = "Nie można załadować pliku: " + file.getOriginalFilename() + "!";
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
        }
    }

    @GetMapping(value = "/files")
    public ResponseEntity<List<FileInfo>> getListFiles() {
        List<FileInfo> fileInfos = filesStorageService.loadAll().map(path -> {
            String filename = path.getFileName().toString();
            String url = MvcUriComponentsBuilder
                    .fromMethodName(com.oskarro.oskarito.storage.FilesController.class, "getFile", path.getFileName().toString()).build().toString();
            return new FileInfo(filename, url);
        }).collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(fileInfos);
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        Resource file = filesStorageService.load(filename, "sss");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @GetMapping(value = "/{username}/avatar")
    @ResponseBody
    @Transactional
    public ResponseEntity<Resource> getImage(@PathVariable String username) throws NotFoundException {
        Image image = imageRepository.findByUserUsername(username)
                .orElseThrow(() -> new NotFoundException("Image not found for user: " + username));
        Resource file = filesStorageService.load(image.getName(), username);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(file);
    }

}
