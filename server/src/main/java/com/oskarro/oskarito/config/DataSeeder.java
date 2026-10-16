package com.oskarro.oskarito.config;

import com.oskarro.oskarito.crawler.CrawlerService;
import com.oskarro.oskarito.playlist.Playlist;
import com.oskarro.oskarito.playlist.PlaylistRepository;
import com.oskarro.oskarito.provider.Provider;
import com.oskarro.oskarito.provider.ProviderRepository;
import com.oskarro.oskarito.provider.contractor.AppleService;
import com.oskarro.oskarito.provider.contractor.BillboardService;
import com.oskarro.oskarito.provider.contractor.RadiopartyService;
import com.oskarro.oskarito.track.Genre;
import com.oskarro.oskarito.track.Track;
import com.oskarro.oskarito.track.TrackRepository;
import com.oskarro.oskarito.user.Role;
import com.oskarro.oskarito.user.RoleName;
import com.oskarro.oskarito.user.RoleRepository;
import com.oskarro.oskarito.user.User;
import com.oskarro.oskarito.user.UserRepository;
import com.oskarro.oskarito.video.Category;
import com.oskarro.oskarito.video.Video;
import com.oskarro.oskarito.video.VideoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class DataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProviderRepository providerRepository;
    private final PlaylistRepository playlistRepository;
    private final VideoRepository videoRepository;
    private final TrackRepository trackRepository;
    private final PasswordEncoder passwordEncoder;
    private final CrawlerService crawlerService;
    private final RadiopartyService radiopartyService;
    private final BillboardService billboardService;
    private final AppleService appleService;

    public DataSeeder(UserRepository userRepository,
                      RoleRepository roleRepository,
                      ProviderRepository providerRepository,
                      PlaylistRepository playlistRepository,
                      VideoRepository videoRepository,
                      TrackRepository trackRepository,
                      PasswordEncoder passwordEncoder,
                      CrawlerService crawlerService,
                      RadiopartyService radiopartyService,
                      BillboardService billboardService,
                      AppleService appleService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.providerRepository = providerRepository;
        this.playlistRepository = playlistRepository;
        this.videoRepository = videoRepository;
        this.trackRepository = trackRepository;
        this.passwordEncoder = passwordEncoder;
        this.crawlerService = crawlerService;
        this.radiopartyService = radiopartyService;
        this.billboardService = billboardService;
        this.appleService = appleService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        // Seed accounts get random passwords; use "forgot password" to take them over.
        String seedPassword = UUID.randomUUID().toString();
        Role roleAdmin = roleRepository.save(role(RoleName.ROLE_ADMIN));
        roleRepository.save(role(RoleName.ROLE_MODERATOR));
        Role roleUser = roleRepository.save(role(RoleName.ROLE_USER));

        User userAdmin = userRepository.save(User.builder()
                .username("Oskarro")
                .email("oskar.slyk@gmail.com")
                .password(passwordEncoder.encode(seedPassword))
                .roles(new HashSet<>(List.of(roleAdmin)))
                .build());
        User userJacek = userRepository.save(User.builder()
                .username("Jacek")
                .email("jacek@pw.pl")
                .password(passwordEncoder.encode(seedPassword))
                .roles(new HashSet<>(List.of(roleUser)))
                .build());

        providerRepository.save(Provider.builder().description("nice").url("https://nuteczki.eu/top20/#").name("nuteczki").genres(new ArrayList<>(List.of(Genre.CLUB, Genre.DANCE))).build());
        Provider radioparty = providerRepository.save(Provider.builder().description("very nice").url("https://radioparty.pl/partylista.html").genres(new ArrayList<>(List.of(Genre.CLUB))).name("radioparty").build());
        providerRepository.save(Provider.builder().description("sehr gut").url("https://www.dance-charts.de/").name("dancecharts").build());
        Provider billboard = providerRepository.save(Provider.builder().description("beautiful").url("https://www.billboard.com/charts/year-end/2019/dance-club-songs").genres(new ArrayList<>(List.of(Genre.DANCE))).name("billboard").build());
        providerRepository.save(Provider.builder().description("super woop").url("https://promodj.com/top100/").name("promodj").build());
        providerRepository.save(Provider.builder().description("bombastic").url("https://musiclist.com/en/").name("musiclist").build());
        providerRepository.save(Provider.builder().description("nicename").url("https://www.ariacharts.com.au/").name("ariacharts").build());
        Provider apple = providerRepository.save(Provider.builder().description("tasty service").url("https://music.apple.com/").genres(new ArrayList<>(List.of(Genre.CLUB, Genre.RETRO))).name("apple").build());

        Playlist playlist = playlistRepository.save(Playlist.builder().name("MyTop").user(userAdmin).build());
        Playlist playlist2 = playlistRepository.save(Playlist.builder().name("SecondTop").user(userJacek).build());

        Video video1 = videoRepository.save(Video.builder().name("Vixa").url("Dp--txMIGPI").category(Category.MIX.toString()).build());
        Video video2 = videoRepository.save(Video.builder().name("Virus").url("MpWfj-2P-9M").category(Category.MIX.toString()).build());
        videoRepository.save(Video.builder().name("L'Italiano").url("moFuKK_Ac").category(Category.RETRO.name()).build());
        videoRepository.save(Video.builder().name("Luna Mix Vol. 9").url("WRooj5n80uo").category(Category.LUNA_MIX.name()).build());

        trackRepository.save(Track.builder().title("This is my test").artist("Mega test").version("Radio edit").playlist(playlist).build());
        trackRepository.save(Track.builder().title("next tes").artist("Mega test").version("Radio edit").playlist(playlist).build());
        trackRepository.save(Track.builder().title("This is More than ntht").artist("Mega tdsdsest").version("Remix").playlist(playlist2).build());
        trackRepository.save(Track.builder().title("This is my test").artist("Mega oss").version("dsd edit").playlist(playlist).build());
        trackRepository.save(Track.builder().title("L'Italiano").artist("The Sicilians ft. Angelo Venuto").version("The DJ Serg Remix").url("https://www.youtube.com/watch?v=hymoFuKK_Ac").playlist(playlist).build());
        trackRepository.save(Track.builder().title("First shit title").artist("Med").version("dsd edit").video(video1).build());
        trackRepository.save(Track.builder().title("Firseconddtitle").artist("Msssed").version("dsd edit").video(video2).build());
        trackRepository.save(Track.builder().title("one tow three").artist("test").version("dsd edit").video(video2).build());

        importCharts(radioparty, billboard, apple);
    }

    private void importCharts(Provider radioparty, Provider billboard, Provider apple) {
        try {
            crawlerService.parseWeb(apple);
            radiopartyService.getTrackList(radioparty);
            billboardService.getTrackList(billboard);
            appleService.getTracklistByGenre(apple, Genre.RETRO);
            appleService.getTracklistByGenre(apple, Genre.TRANCE);
            appleService.getTracklistByGenre(apple, Genre.CLUB);
        } catch (RuntimeException ex) {
            log.warn("External chart import failed, local seed data is still available: {}", ex.toString());
        }
    }

    private Role role(RoleName name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }
}
