import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { RefreshAfterHttpDirective, UiRefresh } from '../services/ui-refresh';
import { TrackService } from '../services/track/track.service';
import { TrackSearch } from '../services/track-search';
import { genreLabel } from '../tracks/genres';
import { LangService } from '../lang.service';

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class SearchComponent implements OnInit {

  query = '';
  catalog: Array<any> = [];
  private loadedQuery = '';
  page = 1;
  readonly pageSize = 10;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private trackService: TrackService,
              private search: TrackSearch,
              private uiRefresh: UiRefresh,
              readonly lang: LangService) {}

  ngOnInit() {
    this.route.queryParamMap.subscribe(params => {
      this.query = (params.get('q') || '').trim();
      this.search.text = this.query;
      const page = Number(params.get('strona'));
      this.page = Number.isInteger(page) && page > 0 ? page : 1;
      this.load();
      this.uiRefresh.refresh();
    });

  }

  get results() {
    return this.query ? this.catalog : [];
  }

  /** Searching happens on the server (title, artist, version, event, tracklist, genre, who added it). */
  private load() {
    if (!this.query || this.query === this.loadedQuery) {
      return;
    }
    const query = this.query;
    this.trackService.searchTracks(query).subscribe(tracks => {
      if (query === this.query) {
        this.catalog = tracks || [];
        this.loadedQuery = query;
      }
    });
  }

  get pageCount() {
    return Math.max(1, Math.ceil(this.results.length / this.pageSize));
  }

  get currentPage() {
    return Math.min(this.page, this.pageCount);
  }

  get pageResults() {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.results.slice(start, start + this.pageSize);
  }

  showPage(page: number) {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {strona: page > 1 ? page : null},
      queryParamsHandling: 'merge'
    });
    window.scrollTo({top: 0, behavior: 'smooth'});
  }

  genreName(genre: string) {
    return genreLabel(genre);
  }

  cover(track) {
    return this.trackService.coverUrl(track.id);
  }

}
