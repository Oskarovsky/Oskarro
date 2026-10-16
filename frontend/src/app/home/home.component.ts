import { Component, OnInit } from '@angular/core';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';
import {TokenStorageService} from '../services/auth/token-storage.service';
import {TrackService} from '../services/track/track.service';
const GENRE_NAMES: Record<string, string> = {
  CLUB: 'Club', DANCE: 'Dance', HOUSE: 'House', RETRO: 'Retro'
};

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

  constructor(private tokenStorage: TokenStorageService,
              private trackService: TrackService) { }

  ngOnInit() {
    if (this.tokenStorage.getToken()) {
      this.isLoggedIn = true;
      this.currentUser = this.tokenStorage.getUser();
    }
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
    return GENRE_NAMES[genre] || genre;
  }

  cover(track) {
    return this.trackService.coverUrl(track.id);
  }

}
