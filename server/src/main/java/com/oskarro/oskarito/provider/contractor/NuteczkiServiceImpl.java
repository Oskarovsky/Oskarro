package com.oskarro.oskarito.provider.contractor;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.oskarro.oskarito.crawler.CrawlerService;
import com.oskarro.oskarito.provider.Provider;
import com.oskarro.oskarito.provider.ProviderService;
import com.oskarro.oskarito.track.Genre;
import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class NuteczkiServiceImpl implements NuteczkiService {

    CrawlerService crawlerService;
    TrackService trackService;
    ProviderService providerService;

    public NuteczkiServiceImpl(CrawlerService crawlerService, TrackService trackService, ProviderService providerService) {
        this.crawlerService = crawlerService;
        this.trackService = trackService;
        this.providerService = providerService;
    }

    @Override
    public String getTrackList(Provider provider) {
        providerService.save(provider);
        try {
            Document document =  Jsoup.connect(provider.getUrl()).get();
            Elements views = document.getElementsByClass("view");
            for (Element view : views) {
                Element element = view.getElementsByTag("a").first();
                Element element1 = element.getElementsByTag("img").first();
                System.out.println(element1.attr("alt"));
                System.out.println(element.attr("href"));
                Track track = Track.builder()
                        .title(element1.attr("alt"))
                        .artist(element.attr("href"))
                        .build();
                trackService.saveTrack(track);
            }
            return "All tracklist has been fetched from nuteczki.eu";
        } catch (IOException e) {
//            log.error(String.format("There are a problem with parsing website: %s", provider.getName()));
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getTracklistByGenre(Provider provider, Genre genre) {
        try {
            final WebClient webClient = new WebClient();
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            final HtmlPage page = webClient.getPage(provider.getUrl());

            HtmlButton select = page.getFirstByXPath( "//div[@class='btn-group category']//button");
            select.click();

            HtmlListItem htmlElement;
            switch (genre.toString()) {
                case "disco":
                    htmlElement = page.getFirstByXPath( "//div[@class='btn-group category open']//ul//li[3]");
                    break;
                case "club":
                    htmlElement = page.getFirstByXPath( "//div[@class='btn-group category open']//ul//li[4]");
                    break;
                case "set":
                    htmlElement = page.getFirstByXPath( "//div[@class='btn-group category open']//ul//li[5]");
                    break;
                case "other":
                    htmlElement = page.getFirstByXPath( "//div[@class='btn-group category open']//ul//li[6]");
                    break;
                default:
                    htmlElement = page.getFirstByXPath( "//div[@class='btn-group category open']//ul//li[0]");
            }

            htmlElement.setAttribute("class", "active");
            System.out.println(htmlElement.getAttribute("data-category"));
            htmlElement.click();

            webClient.waitForBackgroundJavaScript(3 * 1000);

            final List<HtmlElement> spanElements = page.getByXPath("//span[@class='news-title']//a");

            for (Object obj : spanElements) {
                HtmlAnchor anchor = (HtmlAnchor) obj;
                String name = anchor.getTextContent().trim();
                Track track = Track.builder()
                        .artist(name.split(" - ")[0])
                        .title(name.split(" - ")[1].split("\\(")[0])
                        .provider(provider)
                        .genre(genre.toString())
                        .url(anchor.getHrefAttribute().trim())
                        .build();
                if (name.contains("(")) {
                    track.setVersion(name.substring(name.indexOf("(")+1, name.indexOf(")")));
                } else {
                    track.setVersion("Original Mix");
                }
                trackService.saveTrack(track);
            }
            return String.format("Entire %s tracklist has been fetched from nuteczki.eu", genre);
        } catch (IOException e) {
            e.printStackTrace();
            return "Tracks from nuteczki.eu cannot be fetched";
        }
    }
}
