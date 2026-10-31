import {Injectable, NgZone} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Subject} from 'rxjs';
import {audioRef} from '../services/audio-hosts';
import {TokenStorageService} from '../services/auth/token-storage.service';
import {environment} from '../../environments/environment';

/**
 * hostuje.net serves its files only to its own pages, so our audio element cannot play them:
 * they get the hostuje embed on their page and stay out of queues, the radio and playlists.
 */
export function controllable(track: {url?: string} | null): boolean {
  return audioRef(track?.url || '')?.host !== 'hostuje';
}

export interface PlayableTrack {
  id: number;
  title: string;
  artist: string;
  url: string;
  genre?: string;
  hasCover?: boolean;
  version?: string;
  score?: number;
}

const GUEST_KEY = 'oskarro-guest-track';
const PIN_KEY = 'oskarro-radio-pin';

@Injectable({providedIn: 'root'})
export class PlaybackService {
  readonly changes = new Subject<void>();
  track: PlayableTrack = null;
  queue: PlayableTrack[] = [];
  current = 0;
  duration = 0;
  playing = false;
  failed = false;
  holdNext = false;
  pinned = false;
  fromRadio = false;
  radioOpen = false;
  autoplayNext = true;
  peaks: number[] = [];
  audio: HTMLAudioElement;
  /** Set while a playlist plays from the bottom bar (from its page or the radio). */
  playlist: {id: number, name: string} | null = null;
  /** Playlist in order (true) or shuffled like the genre radio (false). */
  sequential = false;

  private generation = 0;
  private history: PlayableTrack[] = [];
  private goingBack = false;
  private radioPick: number = null;
  private notedGeneration = -1;
  private sourceTries = 0;
  private skipFailures = false;
  private readonly dead = new Set<number>();
  private primed: {id: number, src: string, peaks: number[], duration: number} = null;
  private standby: HTMLAudioElement = null;
  private pendingStandby = '';

  constructor(private http: HttpClient,
              private tokenStorage: TokenStorageService,
              private zone: NgZone) {
    this.audio = this.makeAudio();
    // One sound at a time: any <audio>/<video> on the page that starts playing silences the others,
    // including this service's own player (which lives outside the DOM).
    document.addEventListener('play', event => {
      const target = event.target;
      if (!(target instanceof HTMLMediaElement) || target === this.audio) {
        return;
      }
      this.silenceOthers(target);
      if (!this.audio.paused) {
        this.audio.pause();
      }
    }, true);
    try {
      this.pinned = sessionStorage.getItem(PIN_KEY) === '1';
    } catch {
      this.pinned = false;
    }
    this.loadAutoplay();
  }

  loadAutoplay() {
    if (!this.tokenStorage.getToken()) {
      this.autoplayNext = true;
      return;
    }
    this.http.get<{autoplayNext?: boolean}>(environment.serverUrl + '/profile/me').subscribe({
      next: profile => {
        this.autoplayNext = profile?.autoplayNext !== false;
        this.changes.next();
      }
    });
  }

  setAutoplayNext(on: boolean) {
    this.autoplayNext = on;
    if (!on) {
      this.haltStandby();
    }
    this.changes.next();
  }

  close() {
    this.playlist = null;
    this.sequential = false;
    this.history = [];
    this.skipFailures = false;
    this.pause();
    this.haltStandby();
    this.playing = false;
    this.setPinned(false);
  }

  setPinned(on: boolean) {
    this.pinned = on;
    try {
      if (on) {
        sessionStorage.setItem(PIN_KEY, '1');
      } else {
        sessionStorage.removeItem(PIN_KEY);
      }
    } catch {
      // The tab can still pin for this visit when storage is blocked.
    }
    this.changes.next();
  }

  pause() {
    if (!this.audio.paused) {
      this.audio.pause();
    }
  }

  extendQueue(extra: PlayableTrack[]) {
    if (!this.track) {
      return;
    }
    const seen = new Set(this.queue.map(item => item.id));
    const more = (extra || []).filter(item => item?.id && item.url && controllable(item) && !seen.has(item.id));
    if (!more.length) {
      return;
    }
    if (!seen.has(this.track.id)) {
      this.queue = [this.track, ...this.queue];
    }
    this.queue = [...this.queue, ...more];
    this.primeNext();
    this.changes.next();
  }

  setRadioPool(tracks: PlayableTrack[]) {
    const pool = (tracks || []).filter(item => item?.id && item.url && controllable(item));
    this.queue = pool;
    this.radioPick = null;
    this.primed = null;
    this.haltStandby();
    if (this.fromRadio && this.track && pool.length) {
      this.primeNext();
    }
    this.changes.next();
  }

  private silenceOthers(keep: HTMLMediaElement) {
    document.querySelectorAll('audio, video').forEach(media => {
      if (media !== keep && media instanceof HTMLMediaElement && !media.paused) {
        media.pause();
      }
    });
  }

  private makeAudio() {
    const audio = new Audio();
    audio.preload = 'auto';
    audio.setAttribute('controlsList', 'nodownload');
    audio.addEventListener('timeupdate', () => {
      if (audio !== this.audio) {
        return;
      }
      this.zone.run(() => {
        this.current = audio.currentTime || 0;
        this.changes.next();
      });
    });
    audio.addEventListener('loadedmetadata', () => {
      if (audio !== this.audio) {
        return;
      }
      this.zone.run(() => {
        this.duration = Number.isFinite(audio.duration) ? audio.duration : this.duration;
        this.failed = false;
        this.changes.next();
      });
    });
    audio.addEventListener('play', () => {
      if (audio !== this.audio) {
        return;
      }
      this.silenceOthers(audio);
      if (this.pendingStandby) {
        this.startStandby(this.pendingStandby);
      }
      this.zone.run(() => {
        this.playing = true;
        this.notePlay();
        this.changes.next();
      });
    });
    audio.addEventListener('pause', () => {
      if (audio !== this.audio) {
        return;
      }
      this.zone.run(() => {
        this.playing = false;
        this.changes.next();
      });
    });
    audio.addEventListener('ended', () => {
      if (audio !== this.audio) {
        return;
      }
      this.zone.run(() => this.onEnded());
    });
    audio.addEventListener('error', () => {
      if (audio !== this.audio) {
        return;
      }
      this.zone.run(() => {
        if (audio.error && audio.error.code !== 1) {
          this.failed = true;
          this.playing = false;
          this.changes.next();
        }
      });
    });
    return audio;
  }

  allows(id: number) {
    if (this.tokenStorage.getToken()) {
      return true;
    }
    const kept = sessionStorage.getItem(GUEST_KEY);
    return !kept || kept === String(id);
  }

  get hasNext() {
    return !!this.following();
  }

  open(track: PlayableTrack, queue: PlayableTrack[], asRadio = false) {
    if (!track?.id || !track.url || !controllable(track)) {
      return;
    }
    queue = (queue || []).filter(controllable);
    if (!asRadio && this.pinned && this.fromRadio) {
      return;
    }
    if (!this.allows(track.id)) {
      this.holdNext = true;
      this.changes.next();
      return;
    }
    if (!this.tokenStorage.getToken()) {
      sessionStorage.setItem(GUEST_KEY, String(track.id));
    }
    this.skipFailures = false;
    this.fromRadio = asRadio;
    this.playlist = null;
    this.sequential = false;
    this.start(track, queue);
  }

  /**
   * Plays a whole playlist from the bottom bar: it keeps going across pages, track after track
   * (or shuffled), with previous/next. Guests still get only the one track they opened.
   */
  openPlaylist(tracks: PlayableTrack[], startIndex: number, info: {id: number, name: string}, shuffle = false) {
    const queue = (tracks || []).filter(item => item?.id && item.url && controllable(item));
    if (!queue.length) {
      return;
    }
    const first = shuffle
      ? queue[Math.floor(Math.random() * queue.length)]
      : queue[Math.max(0, Math.min(startIndex || 0, queue.length - 1))];
    if (!this.allows(first.id)) {
      this.holdNext = true;
      this.changes.next();
      return;
    }
    this.skipFailures = false;
    this.fromRadio = true;
    this.playlist = info;
    this.sequential = !shuffle;
    this.history = [];
    this.setPinned(true);
    if (this.track?.id === first.id) {
      this.track = null;
    }
    this.start(first, queue);
  }

  /** Back to the genre radio (or nothing) after a playlist. */
  leavePlaylist() {
    this.playlist = null;
    this.sequential = false;
    this.changes.next();
  }

  get hasPrevious() {
    if (this.history.length) {
      return true;
    }
    return this.sequential && this.queue.findIndex(item => item.id === this.track?.id) > 0;
  }

  previous() {
    if (!this.tokenStorage.getToken()) {
      return;
    }
    let target = this.history.pop() || null;
    if (!target && this.sequential) {
      const index = this.queue.findIndex(item => item.id === this.track?.id);
      target = index > 0 ? this.queue[index - 1] : null;
    }
    if (!target) {
      return;
    }
    this.skipFailures = true;
    this.goingBack = true;
    try {
      this.start(target, this.queue);
    } finally {
      this.goingBack = false;
    }
  }

  private start(track: PlayableTrack, queue: PlayableTrack[]) {
    this.remember(track);
    this.queue = (queue || []).filter(item => item?.id && item.url);
    if (!this.queue.some(item => item.id === track.id)) {
      this.queue = [track, ...this.queue];
    }
    this.holdNext = false;
    if (this.track?.id === track.id && this.audio.src && !this.failed) {
      this.track = track;
      this.changes.next();
      return;
    }
    this.radioPick = null;
    this.track = track;
    this.failed = false;
    this.current = 0;
    this.duration = 0;
    this.peaks = [];
    this.playing = false;
    this.load(track, true);
  }

  leadWithGuest(queue: PlayableTrack[]) {
    if (this.tokenStorage.getToken()) {
      return queue;
    }
    let kept = '';
    try {
      kept = sessionStorage.getItem(GUEST_KEY) || '';
    } catch {
      return queue;
    }
    const index = queue.findIndex(item => String(item.id) === kept);
    if (index <= 0) {
      return queue;
    }
    const next = queue.slice();
    next.unshift(next.splice(index, 1)[0]);
    return next;
  }

  showHeld(track: PlayableTrack, queue: PlayableTrack[]) {
    if (!track?.id) {
      return;
    }
    this.fromRadio = true;
    this.queue = (queue || []).filter(item => item?.id && item.url);
    if (!this.queue.some(item => item.id === track.id)) {
      this.queue = [track, ...this.queue];
    }
    this.skipFailures = false;
    this.holdNext = true;
    this.track = track;
    this.failed = false;
    this.playing = false;
    this.current = 0;
    this.duration = 0;
    this.peaks = [];
    this.pause();
    this.haltStandby();
    this.changes.next();
  }

  toggle() {
    if (this.track && !this.allows(this.track.id)) {
      this.holdNext = true;
      this.changes.next();
      return;
    }
    this.holdNext = false;
    if (this.audio.error && this.audio.src) {
      const src = this.audio.src;
      this.failed = false;
      this.audio.src = src;
      this.playCurrent();
      return;
    }
    if (!this.audio.src) {
      return;
    }
    if (this.audio.paused) {
      this.audio.play().catch(() => {
        this.playing = false;
        this.changes.next();
      });
      return;
    }
    this.audio.pause();
  }

  next() {
    if (!this.tokenStorage.getToken()) {
      this.holdNext = true;
      this.audio.pause();
      this.changes.next();
      return;
    }
    const upcoming = this.following();
    if (!upcoming) {
      return;
    }
    this.skipFailures = true;
    if (this.primed?.id === upcoming.id && this.primed.src) {
      this.startPrimed(upcoming);
      return;
    }
    this.start(upcoming, this.queue);
  }

  seekRatio(ratio: number) {
    if (!this.duration) {
      return;
    }
    const next = Math.min(1, Math.max(0, ratio)) * this.duration;
    this.audio.currentTime = next;
    this.current = next;
    this.changes.next();
  }

  setVolume(value: number) {
    this.audio.volume = value;
  }

  retry() {
    if (this.track) {
      this.failed = false;
      this.load(this.track, true);
    }
  }

  clock(seconds: number) {
    if (!Number.isFinite(seconds) || seconds < 0) {
      return '0:00';
    }
    const whole = Math.floor(seconds);
    const minutes = Math.floor(whole / 60);
    return minutes + ':' + String(whole % 60).padStart(2, '0');
  }

  private onEnded() {
    this.playing = false;
    if (!this.tokenStorage.getToken()) {
      this.holdNext = true;
      this.changes.next();
      return;
    }
    const radioQueue = this.fromRadio && (this.pinned || this.radioOpen);
    if (!radioQueue || (!this.autoplayNext && !this.playlist)) {
      this.haltStandby();
      this.changes.next();
      return;
    }
    const upcoming = this.following();
    if (!upcoming) {
      this.changes.next();
      return;
    }
    this.skipFailures = true;
    if (this.primed?.id === upcoming.id && this.primed.src) {
      this.startPrimed(upcoming);
      return;
    }
    this.start(upcoming, this.queue);
  }

  private playCurrent() {
    this.audio.play().catch(error => {
      if (error?.name === 'AbortError') {
        this.audio.play().catch(() => this.changes.next());
        return;
      }
      if (error?.name !== 'NotAllowedError') {
        this.failed = true;
      }
      this.playing = false;
      this.changes.next();
    });
  }

  private remember(next: PlayableTrack) {
    if (this.goingBack || !this.track || this.track.id === next?.id) {
      return;
    }
    this.history.push(this.track);
    if (this.history.length > 50) {
      this.history.shift();
    }
  }

  private startPrimed(track: PlayableTrack) {
    this.remember(track);
    const ready = this.primed;
    this.primed = null;
    this.track = track;
    this.failed = false;
    this.holdNext = false;
    this.current = 0;
    this.peaks = ready?.peaks || [];
    this.duration = ready?.duration || 0;
    this.generation++;
    this.pendingStandby = '';
    const warmed = this.standby && this.standby.src === ready.src ? this.standby : null;
    this.standby = null;
    if (warmed) {
      const volume = this.audio.volume || 1;
      this.audio.pause();
      this.audio = warmed;
      this.audio.volume = volume;
      if (this.audio.currentTime > 0.2) {
        this.audio.currentTime = 0;
      }
      this.playing = !this.audio.paused;
      if (this.audio.paused) {
        this.playCurrent();
      } else {
        this.notePlay();
      }
    } else {
      this.audio.src = ready.src;
      this.playCurrent();
    }
    this.primeNext();
    this.changes.next();
  }

  private haltStandby() {
    this.pendingStandby = '';
    if (!this.standby) {
      return;
    }
    this.standby.pause();
    this.standby.src = '';
    this.standby = null;
  }

  private arm(src: string) {
    if (!src) {
      return;
    }
    this.pendingStandby = src;
    if (!this.audio.paused) {
      this.startStandby(src);
    }
  }

  private startStandby(src: string) {
    if (!src || this.standby?.src === src) {
      return;
    }
    const standby = this.makeAudio();
    standby.volume = 0;
    standby.src = src;
    this.standby = standby;
    standby.play().catch(() => {
      if (this.standby === standby) {
        this.standby = null;
      }
    });
  }

  private primeNext() {
    const upcoming = this.following();
    if (!upcoming || this.primed?.id === upcoming.id) {
      return;
    }
    const ref = audioRef(upcoming.url);
    if (!ref) {
      return;
    }
    const path = ref.host + '/' + encodeURIComponent(ref.id);
    const id = upcoming.id;
    this.http.get<{src: string}>(environment.serverUrl + '/tracks/play/' + path).subscribe({
      next: source => {
        if (!source?.src || this.following()?.id !== id) {
          return;
        }
        const primed = this.primed?.id === id ? this.primed : {id, src: source.src, peaks: [], duration: 0};
        primed.src = source.src;
        this.primed = primed;
        if (this.autoplayNext && this.fromRadio) {
          this.arm(source.src);
        }
      },
      error: () => {
        if (this.following()?.id !== id) {
          return;
        }
        this.dead.add(id);
        if (this.primed?.id === id) {
          this.primed = null;
        }
        this.primeNext();
      }
    });
    this.http.get<{duration: number, peaks: number[]}>(environment.serverUrl + '/tracks/peaks/' + path).subscribe(wave => {
      if (this.following()?.id !== id) {
        return;
      }
      const primed = this.primed?.id === id ? this.primed : {id, src: '', peaks: [], duration: 0};
      primed.peaks = wave?.peaks || [];
      primed.duration = wave?.duration || 0;
      this.primed = primed;
    });
  }

  private notePlay() {
    if (!this.track?.id || this.notedGeneration === this.generation) {
      return;
    }
    this.notedGeneration = this.generation;
    const id = this.track.id;
    this.http.post(environment.serverUrl + '/tracks/' + id + '/view', {}).subscribe({error: () => {}});
  }

  private following() {
    if (this.fromRadio && !this.sequential) {
      return this.randomFollowing();
    }
    let index = this.queue.findIndex(item => item.id === this.track?.id);
    if (index < 0) {
      return null;
    }
    while (index + 1 < this.queue.length) {
      index++;
      if (!this.dead.has(this.queue[index].id)) {
        return this.queue[index];
      }
    }
    return null;
  }

  private randomFollowing() {
    const choices = this.queue.filter(item => item?.id && item.id !== this.track?.id && !this.dead.has(item.id));
    if (!choices.length) {
      return null;
    }
    const kept = choices.find(item => item.id === this.radioPick);
    if (kept) {
      return kept;
    }
    const pick = choices[Math.floor(Math.random() * choices.length)];
    this.radioPick = pick.id;
    return pick;
  }

  private failCurrent() {
    if (this.track?.id) {
      this.dead.add(this.track.id);
    }
    const upcoming = this.skipFailures ? this.following() : null;
    if (upcoming) {
      this.start(upcoming, this.queue);
      return;
    }
    this.failed = true;
    this.playing = false;
    this.changes.next();
  }

  private load(track: PlayableTrack, autoplay: boolean) {
    const ref = audioRef(track.url);
    const generation = ++this.generation;
    this.sourceTries = 0;
    if (!ref) {
      this.failed = true;
      this.changes.next();
      return;
    }
    const path = ref.host + '/' + encodeURIComponent(ref.id);
    this.http.get<{duration: number, peaks: number[]}>(environment.serverUrl + '/tracks/peaks/' + path).subscribe(wave => {
      if (generation !== this.generation) {
        return;
      }
      this.peaks = wave?.peaks || [];
      if (!this.duration && wave?.duration) {
        this.duration = wave.duration;
      }
      this.changes.next();
    });
    this.fetchSource(path, generation, autoplay);
    this.changes.next();
  }

  private fetchSource(path: string, generation: number, autoplay: boolean) {
    this.http.get<{src: string}>(environment.serverUrl + '/tracks/play/' + path).subscribe({
      next: source => {
        if (generation !== this.generation || !source?.src) {
          return;
        }
        this.audio.src = source.src;
        if (autoplay) {
          this.playCurrent();
        }
        this.primeNext();
        this.changes.next();
      },
      error: () => {
        if (generation !== this.generation) {
          return;
        }
        if (this.sourceTries < 2) {
          this.sourceTries++;
          window.setTimeout(() => this.fetchSource(path, generation, autoplay), 700);
          return;
        }
        this.failCurrent();
      }
    });
  }
}
