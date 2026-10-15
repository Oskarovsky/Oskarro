package com.oskarro.oskarito.crawler;

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

@Service
@Slf4j
public class CrawlerService {

    TrackService trackService;

    public CrawlerService(TrackService trackService) {
        this.trackService = trackService;
    }

    public String getWeb(Provider provider, Genre genre) {
        return null;
    }

    public String parseWeb(Provider provider) {
        String urlSite = "gb/album/big-tunes-ministry-of-sound/1248332428?ign-mpt=uo%3D2";
        try {
            Elements formsList = Jsoup.connect(provider.getUrl() + urlSite)
                    .get()
                    .getElementsByClass("song-name-wrapper");

            for (Element element : formsList) {
                if (element.getElementsByClass("by-line").isEmpty()) {
                    continue;
                }
                Track track = Track.builder()
                        //.position(Integer.valueOf(element.getElementsByTag("span").last().text()))
                        .title(element.getElementsByClass("song-name").first().text())
                        .artist(element.getElementsByClass("by-line").first().text())
                        .provider(provider)
                        .build();
                trackService.saveTrack(track);
            }
            return "All tracklist has been fetched from ariacharts.com";
        } catch (IOException e) {
            log.error(String.format("There are a problem with parsing website: %s", provider.getName()));
            e.printStackTrace();
            return null;
        }
    }
}
