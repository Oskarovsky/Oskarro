import {ChangeDetectorRef, Component, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs';
import {TrackService} from '../../services/track/track.service';
import {TokenStorageService} from '../../services/auth/token-storage.service';
import {audioRef} from '../../services/audio-hosts';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import {environment} from '../../../environments/environment';
import {GENRES} from '../genres';
import {PlaybackService, PlayableTrack} from '../playback.service';
import {Track} from '../track/model/track';
import {LangService} from '../../lang.service';

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
  private sub: Subscription;

  constructor(readonly playback: PlaybackService,
              private tracks: TrackService,
              private tokenStorage: TokenStorageService,
              private view: ChangeDetectorRef,
              readonly lang: LangService) {}

  ngOnInit() {
    this.playback.radioOpen = true;
    this.themes = this.readThemes();
    this.sub = this.playback.changes.subscribe(() => this.view.detectChanges());
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

  toggleTheme(id: string) {
    if (this.themes.has(id)) {
      this.themes.delete(id);
    } else {
      this.themes.add(id);
    }
    this.themes = new Set(this.themes);
    this.saveThemes();
    this.retune();
  }

  clearThemes() {
    if (!this.themes.size) {
      return;
    }
    this.themes = new Set();
    this.saveThemes();
    this.retune();
  }

  private useCatalog(all: Track[]) {
    this.catalogAll = this.catalog(all);
    this.retune();
  }

  private retune() {
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
    return all.filter(item => item?.id && item.url && item.genre && !!audioRef(item.url));
  }
}
