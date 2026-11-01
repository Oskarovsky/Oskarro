import {ChangeDetectorRef, Component, OnDestroy, OnInit, ElementRef, HostListener, ViewChild} from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';

import {TrackService} from "../../services/track/track.service";
import {Track} from "./model/track";
import {TokenStorageService} from "../../services/auth/token-storage.service";
import {audioRef} from "../../services/audio-hosts";
import {genreLabel, genreSlug} from "../genres";
import {setCategoryById} from "../sets";
import {SeoService} from "../../seo.service";
import {PlaybackService, PlayableTrack} from "../playback.service";
import {PlaylistService} from "../../services/playlist/playlist.service";
import {LangService} from "../../lang.service";

@Component({
  selector: 'app-track',
  templateUrl: './track.component.html',
  styleUrls: ['./track.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class TrackComponent implements OnInit, OnDestroy {

  track: any = {};
  tracks: Track[] = [];
  comments: Array<{id: number, text: string, author: string, createdAt: string, avatar?: boolean}> = [];
  likes = 0;
  dislikes = 0;
  score = 0;
  mine = 0;
  voting = false;
  voteNote = '';
  downloading = false;
  downloadNote = '';
  isLoggedIn = false;
  draft = '';
  genrePath = 'club';
  genreLabel = 'Club';

  get isSet() {
    return this.track?.kind === 'set';
  }

  get setLabel() {
    const category = setCategoryById(this.track?.setCategory);
    return category ? (this.lang.en ? category.labelEn : category.label) : '';
  }

  get backLink() {
    return this.isSet ? ['/sety', setCategoryById(this.track?.setCategory)?.slug || ''] : ['/tracklist', this.genrePath];
  }

  get backLabel() {
    return this.isSet ? this.setLabel || this.lang.t('Sety', 'Sets') : this.genreLabel;
  }

  get tracklistLines(): string[] {
    return (this.track?.tracklist || '').split('\n').map(line => line.trim()).filter(line => line.length);
  }
  coverStamp = '';
  similar: PlayableTrack[] = [];
  playlists: Array<{id: number, name: string}> = [];
  saveOpen = false;
  saved = '';
  lista = '';
  besideRadio = false;
  file: {bytes: number, bitrate: number, sampleRate: number, channels: number, bitDepth: number, bpm: number, format: string} | null = null;
  fileState: 'idle' | 'loading' | 'done' = 'idle';

  sub: Subscription;
  private playbackSub: Subscription;

  constructor(private trackService: TrackService,
              private route: ActivatedRoute,
              private tokenStorage: TokenStorageService,
              private seo: SeoService,
              readonly playback: PlaybackService,
              private playlistService: PlaylistService,
              private view: ChangeDetectorRef,
              readonly lang: LangService) { }

  ngOnInit() {
    this.playbackSub = this.playback.changes.subscribe(() => this.view.detectChanges());
    this.isLoggedIn = !!this.tokenStorage.getToken();
    this.sub = this.route.params.subscribe(params => {
      const id = params.id;
      this.lista = this.route.snapshot.queryParamMap.get('lista') || '';
      if (id) {
        this.loadTrack(id);
        this.trackService.getComments(id).subscribe(comments => this.comments = comments || []);
        this.trackService.getLikes(id).subscribe(state => this.applyVote(state));
      }
    });
  }

  ngOnDestroy() {
    this.playbackSub?.unsubscribe();
    this.sub?.unsubscribe();
  }

  get ownsPlayback() {
    return !!this.track?.id && this.playback.track?.id === this.track.id;
  }

  get radioPinned() {
    return this.playback.pinned && this.playback.fromRadio;
  }

  get showDeck() {
    return this.ownsPlayback && !this.radioPinned;
  }

  playHere() {
    if (!this.track?.id) {
      return;
    }
    this.besideRadio = false;
    this.startPlayback(this.track);
  }

  loadTrack(id: string) {
    this.trackService.getTrackById(id).subscribe((track: any) => {
      const changed = this.track?.id !== track.id;
      this.track = track;
      this.genrePath = genreSlug(track.genre);
      this.genreLabel = genreLabel(track.genre);
      this.seo.track(track);
      if (changed) {
        this.saved = '';
        this.saveOpen = false;
        this.loadFile(track);
        this.startPlayback(track);
      }
    });
  }

  @ViewChild('saveWrap') saveWrap?: ElementRef<HTMLElement>;

  get isAdmin() {
    return (this.tokenStorage.getUser()?.roles || []).includes('ROLE_ADMIN');
  }

  /** Like the main menu: the playlist list closes once the mouse wanders well away from it. */
  @HostListener('document:pointermove', ['$event'])
  closeSaveWhenFar(event: PointerEvent) {
    if (!this.saveOpen || (event.pointerType && event.pointerType !== 'mouse')) {
      return;
    }
    const wrap = this.saveWrap?.nativeElement;
    if (!wrap) {
      return;
    }
    const boxes = [wrap.getBoundingClientRect()];
    const pop = wrap.querySelector('.save-pop');
    if (pop instanceof HTMLElement) {
      boxes.push(pop.getBoundingClientRect());
    }
    const reach = 72;
    const far = event.clientX < Math.min(...boxes.map(b => b.left)) - reach
      || event.clientX > Math.max(...boxes.map(b => b.right)) + reach
      || event.clientY < Math.min(...boxes.map(b => b.top)) - reach
      || event.clientY > Math.max(...boxes.map(b => b.bottom)) + reach;
    if (far) {
      this.saveOpen = false;
      this.view.detectChanges();
    }
  }

  @HostListener('document:click', ['$event'])
  closeSaveOnOutsideClick(event: MouseEvent) {
    const wrap = this.saveWrap?.nativeElement;
    if (this.saveOpen && wrap && !wrap.contains(event.target as Node)) {
      this.saveOpen = false;
    }
  }

  toggleSave() {
    this.saveOpen = !this.saveOpen;
    if (!this.saveOpen || this.playlists.length) {
      return;
    }
    const username = this.tokenStorage.getUser()?.username;
    if (!username) {
      return;
    }
    this.playlistService.getAllPlaylistsByUsername(username).subscribe(lists => {
      this.playlists = lists || [];
    });
  }

  pin(list: {id: number, name: string}) {
    this.playlistService.pin(list.id, this.track.id).subscribe({
      next: saved => {
        this.saved = saved?.name || list.name;
        this.saveOpen = false;
      }
    });
  }

  private startPlayback(track: PlayableTrack) {
    const radioWasPinned = this.playback.pinned && this.playback.fromRadio;
    this.trackService.getAllTracks().subscribe(all => {
      if (this.track?.id !== track.id) {
        return;
      }
      const community = ((all || []) as PlayableTrack[]).filter(item => item?.id && item.url && item.genre);
      this.similar = this.pickSimilar(track, community);
      if (this.hostujeId) {
        // Played in hostuje's own embed on the page; the radio or anything else keeps going.
        this.besideRadio = false;
        return;
      }
      if (radioWasPinned || (this.playback.pinned && this.playback.fromRadio)) {
        this.besideRadio = true;
        return;
      }
      this.besideRadio = false;
      const genreQueue = community
        .filter(item => item.genre === track.genre)
        .sort((a, b) => (b.score || 0) - (a.score || 0) || (b.id || 0) - (a.id || 0));
      if (!this.lista) {
        if (this.playback.allows(track.id)) {
          this.playback.open(track, genreQueue);
        }
        return;
      }
      this.playlistService.getAllTracksFromPlaylist(this.lista).subscribe(rows => {
        const queue = ((rows || []) as PlayableTrack[]).filter(item => item?.id && item.url);
        if (this.playback.allows(track.id)) {
          this.playback.open(track, queue.length ? queue : genreQueue);
        }
      });
    });
  }

  private pickSimilar(track: PlayableTrack, community: PlayableTrack[]) {
    const artist = (track.artist || '').trim().toLowerCase();
    const sameArtist = community.filter(item => item.id !== track.id && (item.artist || '').trim().toLowerCase() === artist);
    const used = new Set(sameArtist.map(item => item.id));
    const sameGenre = community.filter(item => item.id !== track.id && item.genre === track.genre && !used.has(item.id));
    return [...sameArtist, ...sameGenre].slice(0, 4);
  }

  /** The artist line split into linkable names and the separators between them (",", "&", "feat."…). */
  get artistParts(): {text: string, slug: string}[] {
    const raw = this.track?.artist || '';
    if (raw === this.artistPartsFor) {
      return this.artistPartsCache;
    }
    const parts: {text: string, slug: string}[] = [];
    const separator = /(\s*(?:,|&|\+|\s(?:x|feat\.?|ft\.?|featuring|vs\.?|pres\.?|with)\s)\s*)/i;
    for (const piece of raw.split(separator)) {
      if (!piece) {
        continue;
      }
      const name = piece.trim();
      const isSeparator = separator.test(piece) && piece.replace(separator, '') === '';
      const slug = isSeparator ? '' : artistSlug(name);
      parts.push({text: piece, slug: slug.length >= 2 ? slug : ''});
    }
    this.artistPartsFor = raw;
    this.artistPartsCache = parts;
    return parts;
  }

  private artistPartsFor = '';
  private artistPartsCache: {text: string, slug: string}[] = [];

  /** hostuje.net id when the track lives there (shown in their embed player). */
  get hostujeId(): string {
    const ref = audioRef(this.track?.url || '');
    return ref?.host === 'hostuje' ? ref.id : '';
  }

  get playable() {
    return !!audioRef(this.track.url || '');
  }

  get quality() {
    const file = this.file;
    if (!file) {
      return '';
    }
    const parts: string[] = [];
    if (file.format === 'FLAC') {
      parts.push(file.bitDepth ? 'FLAC, ' + file.bitDepth + ' bit' : 'FLAC');
    } else if (file.format === 'VBR') {
      parts.push(file.bitrate ? 'VBR, ' + this.lang.t('około', 'about') + ' ' + file.bitrate + ' kb/s' : 'VBR');
    } else if (file.bitrate) {
      parts.push(file.bitrate + ' kb/s');
    }
    if (file.sampleRate) {
      const locale = this.lang.en ? 'en-GB' : 'pl-PL';
      parts.push((file.sampleRate / 1000).toLocaleString(locale, {maximumFractionDigits: 1}) + ' kHz');
    }
    if (file.channels === 1) {
      parts.push('mono');
    } else if (file.channels === 2) {
      parts.push('stereo');
    }
    return parts.join(', ');
  }

  get fileSize() {
    const bytes = this.file?.bytes;
    if (!bytes) {
      return '';
    }
    const locale = this.lang.en ? 'en-GB' : 'pl-PL';
    if (bytes < 1024 * 1024) {
      return Math.max(1, Math.round(bytes / 1024)).toLocaleString(locale) + ' KB';
    }
    return (bytes / 1024 / 1024).toLocaleString(locale, {minimumFractionDigits: 1, maximumFractionDigits: 1}) + ' MB';
  }

  words(count: number, kind: 'views' | 'comments') {
    if (kind === 'views') {
      return this.lang.plural(count, ['wyświetlenie', 'wyświetlenia', 'wyświetleń'], ['view', 'views']);
    }
    return this.lang.plural(count, ['komentarz', 'komentarze', 'komentarzy'], ['comment', 'comments']);
  }

  private loadFile(track) {
    this.file = null;
    if (!audioRef(track?.url || '')) {
      this.fileState = 'done';
      return;
    }
    this.fileState = 'loading';
    this.trackService.fileFacts(track.id).subscribe({
      next: file => {
        if (this.track?.id === track.id) {
          this.file = file;
          this.fileState = 'done';
        }
      },
      error: () => {
        if (this.track?.id === track.id) {
          this.fileState = 'done';
        }
      }
    });
  }

  coverSrc() {
    if (!this.track.hasCover) {
      return '';
    }
    const stamp = this.coverStamp ? '?t=' + this.coverStamp : '';
    return this.trackService.coverUrl(this.track.id) + stamp;
  }

  coverOf(item: PlayableTrack) {
    return this.trackService.coverUrl(item.id);
  }

  vote(value: number) {
    if (!this.track.id) {
      return;
    }
    if (this.voting) {
      return;
    }
    // One vote per track: the opposite vote has to be taken back first.
    if (this.mine === -value) {
      this.voteNote = this.mine === 1
        ? this.lang.t('Najpierw cofnij +1 — kliknij je jeszcze raz.', 'Take back your +1 first — click it again.')
        : this.lang.t('Najpierw cofnij −1 — kliknij je jeszcze raz.', 'Take back your −1 first — click it again.');
      return;
    }
    this.voting = true;
    this.voteNote = '';
    this.trackService.vote(this.track.id, value).subscribe({
      next: state => {
        this.voting = false;
        this.applyVote(state);
        this.loadTrack(this.track.id);
      },
      error: error => {
        this.voting = false;
        this.voteNote = error?.status === 401
          ? this.lang.t('Sesja wygasła. Zaloguj się ponownie, żeby zagłosować.', 'Your session expired. Sign in again to vote.')
          : (error?.error?.message || this.lang.t('Nie udało się zapisać oceny.', 'Could not save your vote.'));
      }
    });
  }

  get credits(): number {
    return this.track?.downloadCredits || 0;
  }

  get isOwnTrack(): boolean {
    const me = this.tokenStorage.getUser()?.username;
    return !!me && this.track?.sharedBy === me;
  }

  get canDownload(): boolean {
    return !!this.track?.downloadUnlocked || this.credits > 0;
  }

  tokenWord(count: number): string {
    if (count === 1) {
      return 'żeton';
    }
    const tens = count % 100;
    return count % 10 >= 2 && count % 10 <= 4 && (tens < 12 || tens > 14) ? 'żetony' : 'żetonów';
  }

  download() {
    if (this.downloading || !this.track.id) {
      return;
    }
    this.downloading = true;
    this.downloadNote = '';
    // Opened right away, inside the click, so pop-up blockers let it through; filled once the server answers.
    const tab = window.open('', '_blank');
    this.trackService.download(this.track.id).subscribe({
      next: result => {
        this.downloading = false;
        if (tab) {
          tab.opener = null;
          tab.location.href = result.url;
        } else {
          window.location.href = result.url;
        }
        this.track = {...this.track, downloadUnlocked: true, downloadCredits: result.creditsLeft};
      },
      error: error => {
        this.downloading = false;
        tab?.close();
        this.downloadNote = error?.status === 401
          ? this.lang.t('Sesja wygasła. Zaloguj się ponownie.', 'Your session expired. Sign in again.')
          : (error?.error?.message || this.lang.t('Nie udało się pobrać pliku. Spróbuj za chwilę.', 'The download did not start. Try again in a moment.'));
        this.loadTrack(this.track.id);
      }
    });
  }

  addComment() {
    const text = this.draft.trim();
    if (!text || !this.track.id) {
      return;
    }
    this.trackService.addComment(this.track.id, text).subscribe(comment => {
      this.comments = [...this.comments, comment];
      this.draft = '';
      this.loadTrack(this.track.id);
    });
  }

  private applyVote(state) {
    this.likes = state.likes;
    this.dislikes = state.dislikes;
    this.score = state.score;
    this.mine = state.mine;
  }

}

/** Same slug rules as the server (ExploreService.slug): no accents, lower case, dashes. */
export function artistSlug(name: string): string {
  return (name || '').replace(/ł/g, 'l').replace(/Ł/g, 'L').normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-+|-+$)/g, '');
}
