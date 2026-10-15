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
public class AppleServiceImpl implements AppleService {

    TrackService trackService;

    public AppleServiceImpl(TrackService trackService) {
        this.trackService = trackService;
    }

    public String getTrackList(Provider provider, String urlPart, Genre genre) {
        try {
            Elements formsList = Jsoup.connect(provider.getUrl() + urlPart)
                    .get()
                    .getElementsByClass("song-name-wrapper");

            for (Element element : formsList) {
                String title = element
                        .getElementsByClass("song-name").first()
                        .text();
                if (element.getElementsByClass("by-line").isEmpty()) {
                    continue;
                }
                Track track = Track.builder()
                        //.position(Integer.valueOf(element.getElementsByTag("span").last().text()))
                        .title(element
                                .getElementsByClass("song-name").first()
                                .text())
                        .artist(element
                                .getElementsByClass("by-line").first()
                                .text())
                        .provider(provider)
                        .genre(genre.toString())
                        .build();
                if (title.contains("(")) {
                    track.setTitle(title.split("\\(")[0]);
                    track.setVersion(title.substring(title.indexOf("(")+1, title.indexOf(")")));
                } else {
                    track.setVersion("Original Mix");
                }
                trackService.saveTrack(track);
            }
            return "All tracklist has been fetched from music.apple.com";
        } catch (IOException e) {
            log.error(String.format("There are a problem with parsing website: %s", provider.getName()));
            e.printStackTrace();
            return null;
        }
    }

    public String getTracklistByGenre(Provider provider, Genre genre) {
        switch (genre) {
            case TRANCE:
                getTrackList(provider, "gb/album/state-trance-500-mixed-by-armin-van-buuren-paul-oakenfold/430574887?ign-mpt=uo%3D2", genre);
                break;
            case CLUB:
                getTrackList(provider, "gb/album/20-dance-workout-beats-unmixed-workout-music-for-fitness/1496274185?ign-mpt=uo%3D2", genre);
                break;
            case RETRO:
                getTrackList(provider, "gb/album/big-tunes-ministry-of-sound/1248332428?ign-mpt=uo%3D2", genre);
                getTrackList(provider, "gb/album/100-clubland-trance/1485866604?ign-mpt=uo%3D2", genre);
                getTrackList(provider, "gb/album/euphoria-classics-ministry-of-sound/1273776031?ign-mpt=uo%3D2", genre);
                getTrackList(provider, "gb/album/50-best-trance-hits-ever/457877887?ign-mpt=uo%3D2", genre);
                break;
            default:
                log.info(String.format("Scrapper cannot find any tracks assigned %s genre", genre.toString()));
                return null;
        }
        return "success!";
    }
}
