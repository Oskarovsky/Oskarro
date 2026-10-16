import {Component, OnInit} from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import {Playlist} from '../playlist/model/playlist';
import {PlaylistService} from '../../services/playlist/playlist.service';
import {TokenStorageService} from '../../services/auth/token-storage.service';
import {LangService} from '../../lang.service';

@Component({
  selector: 'app-playlist-all',
  templateUrl: './playlist-all.component.html',
  styleUrls: ['./playlist-all.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class PlaylistAllComponent implements OnInit {

  playlists: Playlist[] = [];
  searchText = '';
  loggedIn = false;
  loaded = false;

  constructor(private playlistService: PlaylistService,
              private tokenStorage: TokenStorageService,
              readonly lang: LangService) { }

  ngOnInit() {
    this.loggedIn = !!this.tokenStorage.getToken();
    this.playlistService.getAllPlaylists().subscribe(res => {
      this.playlists = res || [];
      this.loaded = true;
    });
  }

  trackWord(count: number) {
    return this.lang.plural(count || 0, ['utwór', 'utwory', 'utworów'], ['track', 'tracks']);
  }

  get visible(): Playlist[] {
    const text = (this.searchText || '').trim().toLowerCase();
    if (!text) {
      return this.playlists;
    }
    return this.playlists.filter(playlist =>
      (playlist.name || '').toLowerCase().includes(text)
      || (playlist.description || '').toLowerCase().includes(text)
      || (playlist.owner || '').toLowerCase().includes(text));
  }
}
