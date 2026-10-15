package com.oskarro.oskarito.provider.contractor;

import com.oskarro.oskarito.provider.Provider;
import com.oskarro.oskarito.track.Genre;
import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class MusicListServiceImpl implements MusicListService {

    TrackService trackService;

    public MusicListServiceImpl(TrackService trackService) {
        this.trackService = trackService;
    }

    @Override
    public String getTrackList(Provider provider, String urlPart, Genre genre) {
        final String URL_YOUTUBE = "https://www.youtube.com/watch?v=";
        try {
            Elements formsList = Jsoup.connect(provider.getUrl() + urlPart)
                    .get()
                    .getElementsByClass("songs-list-item");

            for (Element element : formsList) {
                JSONObject json = new JSONObject(element.attr("data-player-song"));
                Track track = Track.builder()
                        .title(element.getElementsByTag("h4").text())
                        .artist(element.getElementsByTag("p").text())
                        .url(URL_YOUTUBE + json.get("youtubeId"))
                        .genre(genre.toString())
                        .provider(provider)
                        .build();
                trackService.saveTrack(track);
            }
            return "All tracklist has been fetched from Billboard.com";
        } catch (IOException e) {
            log.error(String.format("There are a problem with parsing website: %s", provider.getName()));
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getTracklistByGenre(Provider provider, Genre genre) {
        switch (genre) {
            case DANCE:
                getTrackList(provider, "top-ventas-mexico/dance", genre);
                getTrackList(provider, "top-ventas-argentina/dance", genre);
                getTrackList(provider, "top-ventas-rusia/dance", genre);
                break;
            case HOUSE:
                getTrackList(provider, "house-2020", genre);
                break;
            case TECHNO:
                getTrackList(provider, "techno", genre);
                break;
            default:
//                log.info(String.format("Scrapper cannot find any tracks assigned %s genre", genre.toString()));
                return null;
        }
        return "success!";
    }
}
