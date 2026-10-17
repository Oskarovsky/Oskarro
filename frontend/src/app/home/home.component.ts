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
  catalog: Array<any> = [];
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
    this.trackService.getAllTracks().subscribe(tracks => {
      this.catalog = (tracks || [])
        .filter(track => track.genre && track.url)
        .sort((a, b) => (b.id || 0) - (a.id || 0));
    });
  }

  get results() {
    return this.catalog.slice(0, 6);
  }

  /** Top three by plays this month, falling back to score, so the card is never empty. */
  get hot() {
    return [...this.catalog]
      .sort((a, b) => (Number(b.viewsMonth) || 0) - (Number(a.viewsMonth) || 0)
        || (Number(b.score) || 0) - (Number(a.score) || 0)
        || (b.id || 0) - (a.id || 0))
      .slice(0, 3);
  }

  setLabel(c: SetCategoryOption) {
    return this.lang.en ? c.labelEn : c.label;
  }

  setCount(id: string) {
    return this.catalog.filter(track => track.kind === 'set' && track.setCategory === id).length;
  }

  countFor(genreId: string) {
    return this.catalog.filter(track => track.genre === genreId && track.kind !== 'set').length;
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
