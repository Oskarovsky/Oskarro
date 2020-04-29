package com.oskarro.oskarito.provider.contractor;

import com.oskarro.oskarito.provider.Provider;
import com.oskarro.oskarito.track.Genre;
import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class PromodjServiceImpl implements PromodjService {

    TrackService trackService;

    public PromodjServiceImpl(TrackService trackService) {
        this.trackService = trackService;
    }

    @Override
    public String getTrackList(Provider provider, String urlPart, Genre genre) {
        try {
            Elements formsList = Jsoup.connect(provider.getUrl() + urlPart)
                    .get()
                    .getElementsByClass("title");

            for (Element element : formsList) {
                String name = element.getElementsByTag("a").text();
                Track track = Track.builder()
                        .url(element.getElementsByTag("a").first().attr("href"))
                        .provider(provider)
                        .build();

                if (name.contains("-")) {
                    track.setArtist(name.split("-")[0]);
                    track.setTitle(name.split("-")[1]);
                } else {
                    track.setTitle(name);
                }

                if (name.contains("(")) {
                    track.setVersion(name.substring(name.indexOf("(")+1, name.indexOf(")")));
                } else {
                    track.setVersion("Original Mix");
                }
                trackService.saveTrack(track);
            }
            return "All tracklist has been fetched from musiclist.com";
        } catch (IOException e) {
            log.error(String.format("There are a problem with parsing website: %s", provider.getName()));
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getTracklistByGenre(Provider provider, Genre genre) {
        switch (genre) {
            case CLUB:
                getTrackList(provider, "tracks/club_house/1y", genre);
                getTrackList(provider, "remixes/club_house/1y", genre);
                break;
            case DANCE:
                getTrackList(provider, "tracks/dance_pop/1y", genre);
                getTrackList(provider, "remixes/dance_pop/1y", genre);
                break;
            case ELECTRO_HOUSE:
                getTrackList(provider, "tracks/electrohouse/1y", genre);
                break;
            case HOUSE:
                getTrackList(provider, "tracks/tracks/house/1y", genre);
                getTrackList(provider, "remixes/deep_house/1y", genre);
                break;
            case TECHNO:
                getTrackList(provider, "tracks/techhouse/1y", genre);
                break;
            case SET:
                getTrackList(provider, "mixes/club_house/1y", genre);
                getTrackList(provider, "mixes/techno/1y", genre);
                break;
            default:
                log.info(String.format("Scrapper cannot find any tracks assigned %s genre", genre.toString()));
                return null;
        }
        return "success!";
    }
}
