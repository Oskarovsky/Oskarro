import {Component, OnInit} from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import {Playlist} from '../playlist/model/playlist';
import {PlaylistService} from '../../services/playlist/playlist.service';
import {TokenStorageService} from '../../services/auth/token-storage.service';
import {LangService} from '../../lang.service';

/** "Moje" for every signed-in user; "Wszystkie" from rank Bywalec (the server enforces it too). */
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
  canBrowseAll = false;
  rank = 'Bywalec';
  tab: 'mine' | 'all' = 'mine';
  note = '';

  constructor(private playlistService: PlaylistService,
              private tokenStorage: TokenStorageService,
              readonly lang: LangService) { }

  ngOnInit() {
    this.loggedIn = !!this.tokenStorage.getToken();
    this.playlistService.access().subscribe(access => {
      this.canBrowseAll = !!access?.canBrowseAll;
      this.rank = access?.rank || this.rank;
    });
    if (this.loggedIn) {
      this.show('mine');
    } else {
      this.loaded = true;
    }
  }

  get lockedNote() {
    return this.lang.en
      ? `All playlists are available from the "${this.rank}" rank up. Add tracks, comment and vote to rank up.`
      : `Wszystkie składanki są dostępne od rangi „${this.rank}” wzwyż. Dodawaj utwory, komentuj i oceniaj, żeby awansować.`;
  }

  show(tab: 'mine' | 'all') {
    this.note = '';
    if (tab === 'mine' && !this.loggedIn) {
      this.note = this.lang.t('Zaloguj się, żeby zobaczyć swoje składanki.', 'Sign in to see your playlists.');
      return;
    }
    if (tab === 'all' && !this.canBrowseAll) {
      this.note = this.lockedNote;
      return;
    }
    this.tab = tab;
    this.loaded = false;
    const request = tab === 'all'
      ? this.playlistService.getAllPlaylists()
      : this.playlistService.getAllPlaylistsByUsername(this.tokenStorage.getUser()?.username);
    request.subscribe({
      next: res => {
        this.playlists = res || [];
        this.loaded = true;
      },
      error: () => {
        this.playlists = [];
        this.loaded = true;
      }
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
