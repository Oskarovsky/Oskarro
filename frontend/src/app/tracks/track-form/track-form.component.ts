import { Component, OnInit } from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import {ActivatedRoute, Router} from '@angular/router';
import {TrackService} from '../../services/track/track.service';
import {TokenStorageService} from '../../services/auth/token-storage.service';
import {AudioHost, audioHostOf, hostujeId, soundfilesId} from '../../services/audio-hosts';
import {Track} from '../track/model/track';
import {GENRES, genreSlug} from '../genres';
import {SET_CATEGORIES, setCategoryById, setCategoryBySlug} from '../sets';

@Component({
  selector: 'app-track-form',
  templateUrl: './track-form.component.html',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class TrackFormComponent implements OnInit {

  genres = GENRES;
  loggedIn = false;
  editing = false;
  closed = false;
  saving = false;
  trackId: number = null;
  existingCover = false;
  editableUntil = '';
  notice = '';
  previewUrl = '';
  coverFile: File = null;
  source: AudioHost = 'hostuje';
  draft = {artist: '', title: '', version: '', url: '', genre: 'CLUB', kind: 'track', setCategory: 'USERS', eventName: '', tracklist: ''};
  readonly setCategories = SET_CATEGORIES;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private trackService: TrackService,
              private tokenStorage: TokenStorageService) { }

  ngOnInit() {
    this.route.params.subscribe(params => {
      this.loggedIn = !!this.tokenStorage.getToken();
      this.editing = !!params.id;
      this.trackId = params.id ? +params.id : null;
      this.closed = false;
      this.notice = '';
      this.coverFile = null;
      this.previewUrl = '';
      const requested = (this.route.snapshot.queryParamMap.get('genre') || 'club').trim().toLowerCase();
      const known = GENRES.find(genre => genre.slug === requested || genre.id.toLowerCase() === requested.replace(/-/g, '_'));
      this.source = 'hostuje';
      const query = this.route.snapshot.queryParamMap;
      const category = setCategoryBySlug(query.get('kategoria') || '');
      this.draft = {artist: '', title: '', version: '', url: '', genre: known ? known.id : 'CLUB',
        kind: query.get('typ') === 'set' ? 'set' : 'track', setCategory: category ? category.id : 'USERS', eventName: '', tracklist: ''};
      if (this.editing && this.loggedIn) {
        this.trackService.getTrackById(String(this.trackId)).subscribe((track: any) => {
          if (!track.editable) {
            this.closed = true;
            this.draft.genre = track.genre || 'CLUB';
            return;
          }
          this.source = audioHostOf(track.url || '');
          this.draft = {
            artist: track.artist || '',
            title: track.title || '',
            version: track.version || '',
            url: track.url || '',
            genre: track.genre || 'CLUB',
            kind: track.kind === 'set' ? 'set' : 'track',
            setCategory: track.setCategory || 'USERS',
            eventName: track.eventName || '',
            tracklist: track.tracklist || ''
          };
          this.existingCover = !!track.hasCover;
          this.editableUntil = track.editableUntil;
        });
      }
    });
  }

  get backGenre() {
    return genreSlug(this.draft.genre);
  }

  get isSet() {
    return this.draft.kind === 'set';
  }

  get backLink() {
    return this.isSet ? ['/sety', setCategoryById(this.draft.setCategory)?.slug || ''] : ['/tracklist', this.backGenre];
  }

  get eventLabel() {
    const category = setCategoryById(this.draft.setCategory);
    return category ? category.eventLabel : 'Wydarzenie';
  }

  coverSrc() {
    if (this.previewUrl) {
      return this.previewUrl;
    }
    if (this.editing && this.existingCover) {
      return this.trackService.coverUrl(this.trackId);
    }
    return '';
  }

  selectCover(event) {
    const file = event.target.files && event.target.files[0];
    if (file && file.size > 2 * 1024 * 1024) {
      this.notice = 'Zdjęcie może mieć najwyżej 2 MB.';
      event.target.value = '';
      return;
    }
    this.coverFile = file || null;
    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
      this.previewUrl = '';
    }
    if (this.coverFile) {
      this.previewUrl = URL.createObjectURL(this.coverFile);
      this.notice = '';
    }
  }

  save() {
    const artist = this.draft.artist.trim();
    const title = this.draft.title.trim();
    const version = this.draft.version.trim();
    const url = this.draft.url.trim();
    if (!artist || !title) {
      this.notice = 'Podaj wykonawcę i tytuł.';
      return;
    }
    const fromHostuje = !!hostujeId(url);
    const fromSoundfiles = /soundfiles\.eu/i.test(url) && !!soundfilesId(url);
    if (this.source === 'hostuje' && !fromHostuje) {
      this.notice = fromSoundfiles
        ? 'Ten link jest z soundfiles.eu. Zmień źródło pliku.'
        : 'Podaj link hostuje.net, na przykład https://hostuje.net/file.php?id= i 32 znaki.';
      return;
    }
    if (this.source === 'soundfiles' && !soundfilesId(url)) {
      this.notice = fromHostuje
        ? 'Ten link jest z hostuje.net. Zmień źródło pliku.'
        : 'Podaj link soundfiles.eu, na przykład https://soundfiles.eu/QN9mExDZG6dW/file.';
      return;
    }
    if (!this.coverFile && !(this.editing && this.existingCover)) {
      this.notice = 'Dodaj zdjęcie utworu. Zapisujemy z niego miniaturę.';
      return;
    }
    const track = {
      artist, title, version, url,
      genre: this.draft.genre,
      kind: this.isSet ? 'set' : 'track',
      setCategory: this.isSet ? this.draft.setCategory : null,
      eventName: this.isSet ? this.draft.eventName.trim() : null,
      tracklist: this.isSet ? this.draft.tracklist.trim() : null,
      id: this.trackId, points: 0, position: null, playlist: null, video: null
    } as Track;
    this.saving = true;
    this.notice = '';
    const request = this.editing
      ? this.trackService.updateTrack(this.trackId, track)
      : this.trackService.shareTrack(track);
    request.subscribe({
      next: saved => this.storeCover(saved.id),
      error: err => {
        this.saving = false;
        this.notice = err?.error?.message || (this.editing
          ? 'Nie udało się zapisać. Poprawki są dostępne tylko dla autora przez 24 godziny.'
          : 'Nie udało się dodać utworu.');
      }
    });
  }

  private storeCover(id: number) {
    if (!this.coverFile) {
      this.router.navigate(['/track', id]);
      return;
    }
    this.trackService.uploadCover(id, this.coverFile).subscribe({
      next: () => this.router.navigate(['/track', id]),
      error: () => {
        this.saving = false;
        this.notice = 'Dane są zapisane, ale zdjęcia nie udało się zmniejszyć. Możesz spróbować ponownie na stronie poprawki.';
        this.router.navigate(['/track', id, 'edit']);
      }
    });
  }

}
