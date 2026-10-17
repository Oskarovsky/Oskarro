import { Component, OnInit } from '@angular/core';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';
import {TokenStorageService} from '../services/auth/token-storage.service';
import {TrackService} from '../services/track/track.service';
import {GENRES, genreLabel} from '../tracks/genres';
import {SiteStats, StatsService} from '../services/stats.service';
import {LangService} from '../lang.service';
import {GenreOption} from '../tracks/genres';
import {SET_CATEGORIES, SetCategoryOption} from '../tracks/sets';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class HomeComponent implements OnInit {

  isLoggedIn = false;
  currentUser: any;
  latest: Array<any> = [];
  hotTracks: Array<any> = [];
  genreCounts: Record<string, number> = {};
  setCounts: Record<string, number> = {};
  genres = GENRES;
  readonly setCategories = SET_CATEGORIES;
  /** Club is the 2x2 hero tile; four wide tiles keep 13 genres packed in a 4-column grid (4 + 4x2 + 8 = 20 cells). */
  readonly wideGenres = ['VIXA', 'DISCO_POLO', 'HANDS_UP', 'POP'];
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
    this.trackService.getHome().subscribe(feed => {
      this.latest = feed.latest || [];
      this.hotTracks = feed.hot || [];
      this.genreCounts = feed.genreCounts || {};
      this.setCounts = feed.setCounts || {};
    });
  }

  get results() {
    return this.latest;
  }

  get hot() {
    return this.hotTracks;
  }

  setLabel(c: SetCategoryOption) {
    return this.lang.en ? c.labelEn : c.label;
  }

  setCount(id: string) {
    return this.setCounts[id] || 0;
  }

  countFor(genreId: string) {
    return this.genreCounts[genreId] || 0;
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
