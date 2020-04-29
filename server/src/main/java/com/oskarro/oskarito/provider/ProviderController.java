package com.oskarro.oskarito.provider;

import com.oskarro.oskarito.provider.contractor.NuteczkiService;
import com.oskarro.oskarito.track.Genre;
import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackService;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping(value = "/api/providers")
@CrossOrigin(origins = "http://localhost:4200")
public class ProviderController {

    com.oskarro.oskarito.provider.ProviderService providerService;
    NuteczkiService nuteczkiService;
    com.oskarro.oskarito.provider.ProviderRepository providerRepository;
    TrackService trackService;

    public ProviderController(com.oskarro.oskarito.provider.ProviderService providerService, NuteczkiService nuteczkiService,
                              com.oskarro.oskarito.provider.ProviderRepository providerRepository, TrackService trackService) {
        this.providerService = providerService;
        this.nuteczkiService = nuteczkiService;
        this.providerRepository = providerRepository;
        this.trackService = trackService;
    }

    @GetMapping(value = "/{provider_id}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    Optional<com.oskarro.oskarito.provider.Provider> getProviderById(@PathVariable Integer provider_id) {
        return providerService.findById(provider_id);
    }

    @GetMapping(value = "/findAll")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    List<com.oskarro.oskarito.provider.Provider> findAll() {
        return providerService.findAll();
    }

    @GetMapping(value = "/{provider_id}/tracks")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    List<Track> getTracksFromProvider(@PathVariable Integer provider_id) {
        return trackService.findByProviderId(provider_id);
    }

    @GetMapping(value = "/{providerName}/all-tracks")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    List<Track> getTracksFromProvider(@PathVariable String providerName) {
        return trackService.findTracksByProviderName(providerName);
    }

    @GetMapping(value = "/{provider_id}/{genre}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    List<Track> getTracksFromProviderByGenre(@PathVariable Integer provider_id,
                                             @PathVariable String genre) {
        return trackService.findByProviderIdAndGenre(provider_id, genre.toUpperCase());
    }

    @PostMapping(value = "/add")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public void addProvider(@RequestBody com.oskarro.oskarito.provider.Provider provider) {
        providerService.save(provider);
    }

    @GetMapping(value = "/{provider_id}/genres")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    Collection<Genre> getAllGenresFromProvider(@PathVariable Integer provider_id) {
        return providerService.getAllGenresFromProvider(provider_id);
    }
}
