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
public class AriaChartsServiceImpl implements AriaChartsService {

    TrackService trackService;

    public AriaChartsServiceImpl(TrackService trackService) {
        this.trackService = trackService;
    }

    @Override
    public String getTrackList(Provider provider, String urlPart, Genre genre) {
        try {
            Elements formsList = Jsoup.connect(provider.getUrl() + urlPart)
                    .get()
                    .getElementById("tbChartItems")
                    .getElementsByTag("tbody").first()
                    .getElementsByTag("tr");

            System.out.println(formsList);

            for (Element element : formsList) {
                String title = element
                        .getElementsByClass("item-title").first()
                        .text();
                Track track = Track.builder()
                        .position(Integer.valueOf(element
                                .getElementsByTag("span").first()
                                .text()))
                        .title(element
                                .getElementsByClass("item-title").first()
                                .text())
                        .artist(element
                                .getElementsByClass("artist-name").first()
                                .text())
                        .genre(genre.toString())
                        .provider(provider)
                        .build();
                if (title.contains("(")) {
                    track.setTitle(title.split("\\(")[0]);
                    track.setVersion(title.substring(title.indexOf("(")+1, title.indexOf(")")));
                } else {
                    track.setVersion("Original Mix");
                }
                trackService.saveTrack(track);


            }
            return "All tracklist has been fetched from www.ariacharts.com.au";
        } catch (IOException e) {
            log.error(String.format("There are a problem with parsing website: %s", provider.getName()));
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getTracklistByGenre(Provider provider, Genre genre) {
        switch (genre) {
            case DANCE:
                getTrackList(provider, "charts/dance-singles-chart", genre);
                break;
            case CLUB:
                getTrackList(provider, "charts/club-tracks-chart", genre);
                break;
            default:
                log.info(String.format("Scrapper cannot find any tracks assigned %s genre", genre.toString()));
                return null;
        }
        return "success!";
    }
}
