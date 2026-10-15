package com.oskarro.oskarito.provider.contractor;


import com.oskarro.oskarito.provider.Provider;
import com.oskarro.oskarito.track.Genre;

public interface NuteczkiService {

    String getTrackList(Provider provider);

    String getTracklistByGenre(Provider provider, Genre genre);

}
