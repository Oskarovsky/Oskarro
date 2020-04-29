package com.oskarro.oskarito.storage;

import com.oskarro.oskarito.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Image {

    @Id
    @GeneratedValue
    private Integer id;

    private String name;

    private String type;

    @Lob
    private byte[] pic;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    private User user;
}

