import {ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs';
import {TrackService} from '../../services/track/track.service';
import {TokenStorageService} from '../../services/auth/token-storage.service';
import {audioRef} from '../../services/audio-hosts';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import {environment} from '../../../environments/environment';
import {GENRES} from '../genres';
import {PlaybackService, PlayableTrack, controllable} from '../playback.service';
import {Track} from '../track/model/track';
import {LangService} from '../../lang.service';
import {PlaylistService} from '../../services/playlist/playlist.service';
import {Playlist} from '../../playlists/playlist/model/playlist';


const THEME_KEY = 'oskarro-radio-themes';

@Component({
  selector: 'app-radio',
  templateUrl: './radio.component.html',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class RadioComponent implements OnInit, OnDestroy {
  loading = false;
  empty = false;
  readonly genres = GENRES;
  themes = new Set<string>();
  private alive = true;
  private catalogAll: PlayableTrack[] = [];
  playlists: Playlist[] = [];
  chosenPlaylist: number | null = null;
  playlistNote = '';
  private sub: Subscription;

  constructor(readonly playback: PlaybackService,
              private tracks: TrackService,
              private playlistService: PlaylistService,
              private tokenStorage: TokenStorageService,
              private view: ChangeDetectorRef,
              readonly lang: LangService) {}

  ngOnInit() {
    this.playback.radioOpen = true;
    this.themes = this.readThemes();
    this.sub = this.playback.changes.subscribe(() => this.view.detectChanges());
    if (this.loggedIn) {
      this.playlistService.playable().subscribe(lists => {
        this.playlists = lists || [];
        this.chosenPlaylist = this.playback.playlist?.id ?? this.playlists[0]?.id ?? null;
      });
    }
    this.loading = !(this.playback.fromRadio && this.playback.track);
    this.tracks.getAllTracks().subscribe({
      next: all => {
        this.loading = false;
        if (!this.alive) {
          return;
        }
        this.useCatalog(all || []);
      },
      error: () => {
        this.loading = false;
        this.empty = !this.playback.track;
      }
    });
  }

  ngOnDestroy() {
    this.alive = false;
    this.playback.radioOpen = false;
    this.sub?.unsubscribe();
  }

  get loggedIn() {
    return !!this.tokenStorage.getToken();
  }

  cover(track: PlayableTrack) {
    return track?.hasCover ? environment.serverUrl + '/tracks/' + track.id + '/cover' : '';
  }

  togglePin() {
    this.playback.setPinned(!this.playback.pinned);
  }

  /** Back from a playlist to the genre radio. */
  backToGenres() {
    this.playback.leavePlaylist();
    this.playback.close();
    this.retune();
  }

  playPlaylist(shuffle: boolean) {
    const chosen = this.playlists.find(list => list.id === Number(this.chosenPlaylist));
    if (!chosen) {
      return;
    }
    this.playlistNote = '';
    this.playlistService.getAllTracksFromPlaylist(String(chosen.id)).subscribe({
      next: tracks => {
        // Unlike the genre radio, a chosen playlist plays whatever it contains, sets included.
        const queue = (tracks || []).filter(item => item?.id && item.url && !!audioRef(item.url) && controllable(item)) as PlayableTrack[];
        if (!queue.length) {
          this.playlistNote = this.lang.t('W tej składance nie ma utworów do odtworzenia.', 'This playlist has no playable tracks.');
          return;
        }
        this.playback.openPlaylist(queue, 0, {id: chosen.id, name: chosen.name}, shuffle);
      },
      error: () => this.playlistNote = this.lang.t('Nie udało się wczytać składanki.', 'Could not load the playlist.')
    });
  }

  toggleTheme(id: string) {
    if (this.themes.has(id)) {
      this.themes.delete(id);
    } else {
      this.themes.add(id);
    }
    this.themes = new Set(this.themes);
    this.saveThemes();
    this.leavePlaylistForGenres();
  }

  clearThemes() {
    if (!this.themes.size) {
      return;
    }
    this.themes = new Set();
    this.saveThemes();
    this.leavePlaylistForGenres();
  }

  private leavePlaylistForGenres() {
    if (this.playback.playlist) {
      this.playback.leavePlaylist();
      this.playback.close();
    }
    this.retune();
  }

  private useCatalog(all: Track[]) {
    this.catalogAll = this.catalog(all);
    this.retune();
  }

  private retune() {
    if (this.playback.playlist) {
      // A playlist is playing from the radio or its page: genre themes don't take over.
      return;
    }
    const pool = this.pool();
    if (!pool.length) {
      this.empty = !this.playback.track;
      if (this.playback.fromRadio) {
        this.playback.setRadioPool([]);
      }
      return;
    }
    this.empty = false;
    if (this.playback.fromRadio && this.playback.track) {
      this.playback.setRadioPool(pool);
      return;
    }
    const start = this.chooseStart(pool);
    if (!start || !this.playback.allows(start.id)) {
      this.playback.showHeld(start || pool[0], pool);
      return;
    }
    this.playback.open(start, pool, true);
  }

  private pool(): PlayableTrack[] {
    if (!this.themes.size) {
      return this.catalogAll;
    }
    return this.catalogAll.filter(item => this.themes.has(item.genre));
  }

  private readThemes(): Set<string> {
    try {
      const raw = JSON.parse(sessionStorage.getItem(THEME_KEY) || '[]');
      const known = new Set(GENRES.map(genre => genre.id));
      return new Set((Array.isArray(raw) ? raw : []).filter(id => known.has(id)));
    } catch {
      return new Set();
    }
  }

  private saveThemes() {
    try {
      if (this.themes.size) {
        sessionStorage.setItem(THEME_KEY, JSON.stringify([...this.themes]));
      } else {
        sessionStorage.removeItem(THEME_KEY);
      }
    } catch {
      // The choice still applies for this visit when storage is blocked.
    }
  }

  private chooseStart(built: PlayableTrack[]): PlayableTrack {
    if (!this.loggedIn) {
      let kept = '';
      try {
        kept = sessionStorage.getItem('oskarro-guest-track') || '';
      } catch {
        kept = '';
      }
      if (kept) {
        return built.find(item => String(item.id) === kept) || null;
      }
    }
    return built[Math.floor(Math.random() * built.length)];
  }

  private catalog(all: Track[]): PlayableTrack[] {
    // Hour-long sets would swamp the radio rotation, so it plays single tracks only.
    return all.filter(item => item?.id && item.url && item.genre && item.kind !== 'set' && !!audioRef(item.url) && controllable(item));
  }
}
