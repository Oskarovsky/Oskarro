package com.oskarro.oskarito.explore;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public data for the genre text, monthly "Nowości" pages and artist pages. */
@RestController
@RequestMapping("/api/explore")
public class ExploreController {

    private final ExploreService explore;

    public ExploreController(ExploreService explore) {
        this.explore = explore;
    }

    @GetMapping("/genre/{slug}")
    ExploreService.GenreInfo genre(@PathVariable String slug) {
        return explore.genreInfo(slug);
    }

    @GetMapping("/month/{slug}/{month}")
    ExploreService.MonthPage month(@PathVariable String slug, @PathVariable String month) {
        return explore.month(slug, month);
    }

    @GetMapping("/artists")
    List<ExploreService.ArtistLink> artists() {
        return explore.artists();
    }

    @GetMapping("/artist/{slug}")
    ExploreService.ArtistPage artist(@PathVariable String slug) {
        return explore.artist(slug);
    }
}
