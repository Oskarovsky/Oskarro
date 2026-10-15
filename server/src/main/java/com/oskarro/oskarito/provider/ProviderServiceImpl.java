package com.oskarro.oskarito.provider;

import com.oskarro.oskarito.crawler.CrawlerService;
import com.oskarro.oskarito.track.Genre;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class ProviderServiceImpl implements ProviderService {

    ProviderRepository providerRepository;
    CrawlerService crawlerService;

    public ProviderServiceImpl(ProviderRepository providerRepository, CrawlerService crawlerService) {
        this.providerRepository = providerRepository;
        this.crawlerService = crawlerService;
    }

    @Override
    public List<com.oskarro.oskarito.provider.Provider> findAll() {
        return providerRepository.findAll();
    }

    @Override
    public Optional<com.oskarro.oskarito.provider.Provider> findById(Integer id) {
        return providerRepository.findById(id);
    }

    @Override
    public com.oskarro.oskarito.provider.Provider save(com.oskarro.oskarito.provider.Provider provider) {
        return providerRepository.save(provider);
    }

    @Override
    public Optional<com.oskarro.oskarito.provider.Provider> findByName(String name) {
        return providerRepository.findByName(name);
    }

    @Override
    public String getCrawler(Integer id) {
        Optional<com.oskarro.oskarito.provider.Provider> foundProvider = findById(id);
        return foundProvider
                .map(provider -> crawlerService.parseWeb(provider))
                .orElse(null);
    }

    @Override
    public Collection<Genre> getAllGenresFromProvider(Integer id) {
        return findById(id)
                .map(com.oskarro.oskarito.provider.Provider::getGenres)
                .orElse(null);
    }

}
