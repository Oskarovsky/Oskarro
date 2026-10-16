import { Component, OnInit } from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import { TrackService} from "../../services/track/track.service";
import {Subscription} from "rxjs";
import {ActivatedRoute} from "@angular/router";
import {TokenStorageService} from "../../services/auth/token-storage.service";

const GENRE_LABELS: Record<string, string> = {club: 'Club', dance: 'Dance', house: 'House', retro: 'Retro'};

@Component({
  selector: 'app-track-list',
  templateUrl: './track-list.component.html',
  styleUrls: ['./track-list.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class TrackListComponent implements OnInit {

  tracks: Array<any> = [];

  genres: Array<any>;

  sub: Subscription;

  isLoggedIn = false;

  community = false;

  genre = '';

  genreLabel = 'Utwory';

  sort: 'score' | 'new' = 'score';

  constructor(private trackService: TrackService,
              private route: ActivatedRoute,
              private tokenStorage: TokenStorageService) {
  }

  ngOnInit() {
    this.isLoggedIn = !!this.tokenStorage.getToken();
    this.sub = this.route.params.subscribe(params => {
      const genre = params.genre;
      this.community = !!genre;
      this.genre = genre || '';
      this.genreLabel = GENRE_LABELS[genre] || (genre ? genre : 'Utwory');
      this.tracks = [];
      if (genre) {
        this.loadGenre();
      }
    })
  }

  setSort(sort: 'score' | 'new') {
    this.sort = sort;
    this.loadGenre();
  }

  coverSrc(track) {
    return this.trackService.coverUrl(track.id);
  }

  private loadGenre() {
    this.trackService.getTracksByGenre(this.genre, this.sort).subscribe((track: any) => {
      this.tracks = track || [];
    });
  }
}
