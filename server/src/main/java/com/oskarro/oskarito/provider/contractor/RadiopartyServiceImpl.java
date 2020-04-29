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
public class RadiopartyServiceImpl implements RadiopartyService {

    TrackService trackService;

    public RadiopartyServiceImpl(TrackService trackService) {
        this.trackService = trackService;
    }

    @Override
    public String getTrackList(Provider provider) {
        try {
            Elements formsList = Jsoup.connect(provider.getUrl())
                    .get()
                    .getElementsByClass("tabcontent").get(0)
                    .getElementsByTag("form");
            for (Element element : formsList) {
                String artist = element.select("div").first().toString()
                        .split("\\n ")[1].split("<")[0]
                        .replaceAll("&amp;","&");
                String title = element.select("div").first()
                        .getElementsByTag("span").first()
                        .text();
                Track track = Track.builder()
                        .artist(artist.substring(artist.indexOf(" ")))
                        .title(title.split("\\(")[0])
                        .genre(Genre.CLUB.toString())
                        .version(title.substring(title.indexOf("(")+1, title.indexOf(")")))
                        .provider(provider)
                        .build();
                trackService.saveTrack(track);
            }
            return "All tracklist has been fetched from radioparty.pl";
        } catch (IOException e) {
            log.error(String.format("There are a problem with parsing website: %s", provider.getName()));
            e.printStackTrace();
            return null;
        }
    }
}
