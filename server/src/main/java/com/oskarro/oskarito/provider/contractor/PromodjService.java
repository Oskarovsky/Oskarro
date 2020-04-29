package com.oskarro.oskarito.provider.contractor;

import com.oskarro.oskarito.provider.Provider;
import com.oskarro.oskarito.track.Genre;

public interface PromodjService {

    String getTrackList(Provider provider, String urlPart, Genre genre);

    String getTracklistByGenre(Provider provider, Genre genre);

}
