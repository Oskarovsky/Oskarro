package com.oskarro.oskarito.track;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.xml.bind.ValidationException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/tracks")
@CrossOrigin(origins = "http://localhost:4200")
public class TrackController {

    private final TrackService trackService;
    private final com.oskarro.oskarito.track.TrackRepository trackRepository;

    public TrackController(TrackService trackService, com.oskarro.oskarito.track.TrackRepository trackRepository) {
        this.trackService = trackService;
        this.trackRepository = trackRepository;
    }

    @GetMapping(value = "/findAll")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    List<Track> findAll() {
        return trackService.findAll();
    }

    @GetMapping(value = "/id/{id}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    Optional<Track> findById(@PathVariable Integer id) {
        return trackService.findById(id);
    }

    @PostMapping(value = "/add")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    void addTrack(@RequestBody Track track, BindingResult bindingResult) throws ValidationException {
        if (bindingResult.hasErrors()) {
            throw new ValidationException("There are a problem with binding");
        }
        trackService.saveTrack(track);
    }

    @GetMapping(value = "/genre/{genre}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    List<Track> findByGenre(@PathVariable String genre) {
        return trackService.findTracksByGenre(genre);
    }

    @PostMapping(value = "/addToRanking")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    void addTrackToRanking (@RequestBody Track track, BindingResult bindingResult) throws ValidationException {
        if (bindingResult.hasErrors()) {
            throw new ValidationException("Track has errors - it cannot by send");
        }
        Track trackAdded = Track.builder()
                .artist(track.getArtist())
                .title(track.getTitle())
                .version(track.getVersion())
                .build();
        trackService.saveTrack(trackAdded);
    }

    @DeleteMapping(value = "/{id}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public void delete(@PathVariable Integer id) {
        this.trackRepository.deleteById(id);
    }

}
