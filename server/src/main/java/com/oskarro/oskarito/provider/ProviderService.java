package com.oskarro.oskarito.provider;

import com.oskarro.oskarito.track.Genre;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProviderService {

    List<com.oskarro.oskarito.provider.Provider> findAll();

    Optional<com.oskarro.oskarito.provider.Provider> findById(Integer id);

    com.oskarro.oskarito.provider.Provider save(com.oskarro.oskarito.provider.Provider provider);

    Optional<com.oskarro.oskarito.provider.Provider> findByName(String name);

    String getCrawler(Integer id);

    Collection<Genre> getAllGenresFromProvider(Integer id);
}
