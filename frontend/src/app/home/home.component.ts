import { Component, OnInit } from '@angular/core';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';
import {TokenStorageService} from '../services/auth/token-storage.service';
import {TrackService} from '../services/track/track.service';
import {GENRES, genreLabel} from '../tracks/genres';
import {SiteStats, StatsService} from '../services/stats.service';
import {LangService} from '../lang.service';
import {GenreOption} from '../tracks/genres';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class HomeComponent implements OnInit {

  isLoggedIn = false;
  currentUser: any;
  catalog: Array<any> = [];
  genres = GENRES;
  stats: SiteStats | null = null;

  constructor(private tokenStorage: TokenStorageService,
              private trackService: TrackService,
              private statsService: StatsService,
              readonly lang: LangService) { }

  ngOnInit() {
    if (this.tokenStorage.getToken()) {
      this.isLoggedIn = true;
      this.currentUser = this.tokenStorage.getUser();
    }
    this.statsService.load().subscribe(stats => this.stats = stats);
    this.trackService.getAllTracks().subscribe(tracks => {
      this.catalog = (tracks || [])
        .filter(track => track.genre && track.url)
        .sort((a, b) => (b.id || 0) - (a.id || 0));
    });
  }

  get results() {
    return this.catalog.slice(0, 6);
  }

  genreName(genre: string) {
    return genreLabel(genre);
  }

  blurb(genre: GenreOption) {
    return this.lang.en ? genre.blurbEn : genre.blurb;
  }

  cover(track) {
    return this.trackService.coverUrl(track.id);
  }

  phrase(count: number, kind: 'visitors' | 'users' | 'tracks' | 'plays') {
    const forms: Record<string, [[string, string, string], [string, string]]> = {
      visitors: [['osoba dziś', 'osoby dziś', 'osób dziś'], ['person today', 'people today']],
      users: [['użytkownik', 'użytkowników', 'użytkowników'], ['user', 'users']],
      tracks: [['utwór', 'utwory', 'utworów'], ['track', 'tracks']],
      plays: [['odtworzenie', 'odtworzenia', 'odtworzeń'], ['play', 'plays']]
    };
    const [pl, en] = forms[kind];
    return this.lang.plural(count, pl, en);
  }

}
