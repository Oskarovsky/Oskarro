import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { RefreshAfterHttpDirective, UiRefresh } from '../services/ui-refresh';
import { TrackService } from '../services/track/track.service';
import { TrackSearch, trackMatches } from '../services/track-search';

const GENRE_NAMES: Record<string, string> = {
  CLUB: 'Club', DANCE: 'Dance', HOUSE: 'House', RETRO: 'Retro'
};

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class SearchComponent implements OnInit {

  query = '';
  catalog: Array<any> = [];

  constructor(private route: ActivatedRoute,
              private trackService: TrackService,
              private search: TrackSearch,
              private uiRefresh: UiRefresh) {}

  ngOnInit() {
    this.route.queryParamMap.subscribe(params => {
      this.query = (params.get('q') || '').trim();
      this.search.text = this.query;
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

  genreName(genre: string) {
    return GENRE_NAMES[genre] || genre;
  }

  cover(track) {
    return this.trackService.coverUrl(track.id);
  }

}
