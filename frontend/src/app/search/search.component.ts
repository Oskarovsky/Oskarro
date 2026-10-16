import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { RefreshAfterHttpDirective, UiRefresh } from '../services/ui-refresh';
import { TrackService } from '../services/track/track.service';
import { TrackSearch, trackMatches } from '../services/track-search';
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
      this.uiRefresh.refresh();
    });
    this.trackService.getAllTracks().subscribe(tracks => {
      this.catalog = (tracks || [])
        .filter(track => track.genre && track.url)
        .sort((a, b) => (b.id || 0) - (a.id || 0));
    });
  }

  get results() {
    if (!this.query) {
      return [];
    }
    return this.catalog.filter(track => trackMatches(track, this.query));
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
