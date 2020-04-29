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
public class BillboardServiceImpl implements BillboardService {

    TrackService trackService;

    public BillboardServiceImpl(TrackService trackService) {
        this.trackService = trackService;
    }

    public String getTrackList(Provider provider) {
        try {
            Elements formsList = Jsoup.connect(provider.getUrl())
                    .get()
                    .getElementsByTag("article");

            for (Element element : formsList) {
                Track track = Track.builder()
                        .title(element.getElementsByClass("ye-chart-item__title").text())
                        .artist(element.getElementsByClass("ye-chart-item__artist").text())
                        .genre(Genre.DANCE.toString())
                        .version("Radio edit")
                        .provider(provider)
                        .build();
                trackService.saveTrack(track);
            }
            return "All tracklist has been fetched from Billboard.com";
        } catch (IOException e) {
            log.error(String.format("There are a problem with parsing website: %s", provider.getName()));
            e.printStackTrace();
            return null;
        }
    }

}
