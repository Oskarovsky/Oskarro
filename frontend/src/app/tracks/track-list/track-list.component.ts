import { Component, OnDestroy, OnInit } from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import { TrackService} from "../../services/track/track.service";
import {Subscription} from "rxjs";
import {ActivatedRoute, Router} from "@angular/router";
import {TokenStorageService} from "../../services/auth/token-storage.service";
import {genreBySlug} from "../genres";
import {LangService} from "../../lang.service";

@Component({
  selector: 'app-track-list',
  templateUrl: './track-list.component.html',
  styleUrls: ['./track-list.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class TrackListComponent implements OnInit, OnDestroy {

  tracks: Array<any> = [];

  page = 1;

  readonly pageSize = 10;

  genres: Array<any>;

  sub: Subscription;

  isLoggedIn = false;

  community = false;

  genre = '';

  genreLabel = 'Utwory';

  private aboutPl = 'Utwory dodane przez społeczność VIXA.';

  private aboutEn = 'Tracks shared by the VIXA community.';

  sort: 'new' | 'score' | 'views-month' | 'views-year' | 'views-all' = 'new';

  private querySub: Subscription;

  constructor(private trackService: TrackService,
              private route: ActivatedRoute,
              private router: Router,
              private tokenStorage: TokenStorageService,
              readonly lang: LangService) {
  }

  get genreAbout() {
    return this.lang.en ? this.aboutEn : this.aboutPl;
  }

  ngOnDestroy() {
    this.sub?.unsubscribe();
    this.querySub?.unsubscribe();
  }

  ngOnInit() {
    this.isLoggedIn = !!this.tokenStorage.getToken();
    this.sub = this.route.paramMap.subscribe(params => {
      const genre = params.get('genre') || '';
      const entered = genre !== this.genre;
      this.community = !!genre;
      this.genre = genre;
      const option = genreBySlug(genre);
      this.genreLabel = option?.label || (genre ? genre : 'Utwory');
      this.aboutPl = option?.about || 'Utwory dodane przez społeczność VIXA.';
      this.aboutEn = option?.aboutEn || 'Tracks shared by the VIXA community.';
      if (!genre) {
        return;
      }
      if (entered) {
        this.sort = 'new';
        this.page = 1;
        this.tracks = [];
        this.dropCarriedFilters();
      }
      this.loadGenre();
    });
    this.querySub = this.route.queryParamMap.subscribe(params => {
      if (params.has('sort')) {
        return;
      }
      this.page = this.readPage(params.get('strona'));
    });
  }

  setSort(sort: 'new' | 'score' | 'views-month' | 'views-year' | 'views-all') {
    if (this.sort === sort) {
      return;
    }
    this.sort = sort;
    this.page = 1;
    const query = this.route.snapshot.queryParamMap;
    if (query.has('strona') || query.has('sort')) {
      this.router.navigate([], {
        relativeTo: this.route,
        queryParams: {strona: null, sort: null},
        queryParamsHandling: 'merge'
      });
    }
    this.loadGenre();
  }

  showPage(page: number) {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {strona: page > 1 ? page : null},
      queryParamsHandling: 'merge'
    });
    window.scrollTo({top: 0, behavior: 'smooth'});
  }

  get pageCount() {
    return Math.max(1, Math.ceil(this.tracks.length / this.pageSize));
  }

  get currentPage() {
    return Math.min(this.page, this.pageCount);
  }

  get pageTracks() {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.tracks.slice(start, start + this.pageSize);
  }

  get monthViews() {
    return this.tracks.reduce((sum, track) => sum + (Number(track.viewsMonth) || 0), 0);
  }

  get newThisWeek() {
    const since = Date.now() - 7 * 24 * 3600 * 1000;
    return this.tracks.filter(track => track.createdAt && Date.parse(track.createdAt) >= since).length;
  }

  get topTrack() {
    const best = this.tracks.reduce((top, track) =>
      (Number(track.viewsMonth) || 0) > (Number(top?.viewsMonth) || 0) ? track : top, null);
    return best && Number(best.viewsMonth) > 0 ? best : null;
  }

  coverSrc(track) {
    return this.trackService.coverUrl(track.id);
  }

  viewsLabel(track): string {
    if (this.sort === 'views-month') {
      return this.viewsPhrase(track?.viewsMonth, 'w tym miesiącu');
    }
    if (this.sort === 'views-year') {
      return this.viewsPhrase(track?.viewsYear, 'w tym roku');
    }
    if (this.sort === 'views-all') {
      return this.viewsPhrase(track?.viewsAll, 'od zawsze');
    }
    return '';
  }

  private dropCarriedFilters() {
    const query = this.route.snapshot.queryParamMap;
    if (!query.has('sort') && !query.has('strona')) {
      return;
    }
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {sort: null, strona: null},
      queryParamsHandling: 'merge',
      replaceUrl: true
    });
  }

  private viewsPhrase(count: number, scope: string) {
    const value = Number(count) || 0;
    const mod10 = value % 10;
    const mod100 = value % 100;
    let word = 'wyświetleń';
    if (mod10 === 1 && mod100 !== 11) {
      word = 'wyświetlenie';
    } else if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
      word = 'wyświetlenia';
    }
    return value + ' ' + word + ' ' + scope;
  }

  private readPage(value: string) {
    const page = Number(value);
    return Number.isInteger(page) && page > 0 ? page : 1;
  }

  private loadGenre() {
    this.trackService.getTracksByGenre(this.genre, this.sort).subscribe((track: any) => {
      this.tracks = track || [];
    });
  }
}
