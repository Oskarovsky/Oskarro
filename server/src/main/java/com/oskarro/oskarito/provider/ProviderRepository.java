package com.oskarro.oskarito.provider;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderRepository extends CrudRepository<com.oskarro.oskarito.provider.Provider, Integer> {

    List<com.oskarro.oskarito.provider.Provider> findAll();

    Optional<com.oskarro.oskarito.provider.Provider> findById(Integer id);

    com.oskarro.oskarito.provider.Provider save(com.oskarro.oskarito.provider.Provider provider);

    Optional<com.oskarro.oskarito.provider.Provider> findByName(String name);

}
