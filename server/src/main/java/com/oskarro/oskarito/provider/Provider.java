package com.oskarro.oskarito.provider;

import com.oskarro.oskarito.track.Genre;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Collection;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Provider {

    @Id
    @GeneratedValue
    private Integer id;

    private String name;

    private String url;

    private String description;

    @ElementCollection(targetClass= Genre.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name="provider_genre")
    @Column(name="genre")
    private Collection<Genre> genres;
}
