import { Component, OnInit } from '@angular/core';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';
import { TokenStorageService } from '../services/auth/token-storage.service';
import {UploadFileService} from '../services/storage/upload-file.service';
import { HttpEventType, HttpResponse, HttpClient } from '@angular/common/http';
import {ActivatedRoute} from '@angular/router';
import {environment} from '../../environments/environment';
import {TrackService} from '../services/track/track.service';
import {PlaybackService} from '../tracks/playback.service';
import {LangService} from '../lang.service';
import {GENRES, genreLabel} from '../tracks/genres';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class ProfileComponent implements OnInit {

  profile: any;
  self = false;
  deleteConfirm = '';

  get externalAccount() {
    return !!this.profile?.signInMethod && this.profile.signInMethod !== 'password';
  }
  deleting = false;
  deleteError = '';
  bio = '';
  firstName = '';
  lastName = '';
  website = '';
  favorite = new Set<string>();
  genres = GENRES;
  grantTitle = '';
  grantUser = '';
  message = '';
  imageToShow: any;
  selectedFile: File = null;
  missing = false;
  loading = false;
  autoplayNext = true;
  trackPage = 1;
  readonly pageSize = 10;

  constructor(private token: TokenStorageService,
              private uploadService: UploadFileService,
              private http: HttpClient,
              private route: ActivatedRoute,
              private trackService: TrackService,
              private playback: PlaybackService,
              readonly lang: LangService) { }

  ngOnInit() {
    this.route.params.subscribe(params => this.load(params.username));
  }

  cover(track) {
    return this.trackService.coverUrl(track.id);
  }

  get trackPages() {
    const total = this.profile?.shared?.length || 0;
    return Math.max(1, Math.ceil(total / this.pageSize));
  }

  get shownTracks() {
    const all = this.profile?.shared || [];
    const page = Math.min(this.trackPage, this.trackPages);
    const start = (page - 1) * this.pageSize;
    return all.slice(start, start + this.pageSize);
  }

  showTracks(page: number) {
    this.trackPage = page;
  }

  saveAutoplay(event: Event) {
    const input = event.target as HTMLInputElement;
    const on = input.checked;
    this.autoplayNext = on;
    this.playback.setAutoplayNext(on);
    this.http.put<any>(environment.serverUrl + '/profile/me', {autoplayNext: on}).subscribe({
      next: profile => {
        this.profile = profile;
        this.autoplayNext = profile.autoplayNext !== false;
        this.playback.setAutoplayNext(this.autoplayNext);
        this.message = 'Ustawienie zapisane.';
      },
      error: () => {
        this.autoplayNext = !on;
        this.playback.setAutoplayNext(this.autoplayNext);
        input.checked = this.autoplayNext;
        this.message = 'Nie udało się zapisać ustawienia.';
      }
    });
  }

  genreName(id: string) {
    return genreLabel(id);
  }

  likedGenres() {
    return (this.profile?.favoriteGenres || '').split(',').map((item: string) => item.trim()).filter(Boolean);
  }

  likedNames() {
    return this.likedGenres().map((id: string) => genreLabel(id)).join(', ');
  }

  toggleGenre(id: string) {
    if (this.favorite.has(id)) {
      this.favorite.delete(id);
    } else {
      this.favorite.add(id);
    }
  }

  saveBio() {
    const website = this.website.trim();
    if (website && !/^https?:\/\//i.test(website)) {
      this.message = 'Strona musi zaczynać się od https://';
      return;
    }
    this.http.put<any>(environment.serverUrl + '/profile/me', {
      bio: this.bio,
      firstName: this.firstName.trim(),
      lastName: this.lastName.trim(),
      website,
      favoriteGenres: [...this.favorite].join(',')
    }).subscribe({
      next: profile => {
        this.profile = profile;
        this.message = 'Zapisane.';
      },
      error: () => this.message = 'Nie udało się zapisać opisu.'
    });
  }

  exportData() {
    this.http.get(environment.serverUrl + '/profile/me/export').subscribe(data => {
      const blob = new Blob([JSON.stringify(data, null, 2)], {type: 'application/json'});
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = 'oskarro-moje-dane.json';
      link.click();
      setTimeout(() => URL.revokeObjectURL(link.href), 1000);
    });
  }

  deleteAccount() {
    if (!confirm(this.lang.t('Na pewno usunąć konto? Tego nie da się cofnąć.', 'Really delete the account? This cannot be undone.'))) {
      return;
    }
    this.deleting = true;
    this.deleteError = '';
    this.http.request('DELETE', environment.serverUrl + '/profile/me', {body: {confirm: this.deleteConfirm}}).subscribe({
      next: () => {
        this.token.signOut();
        window.location.assign('/?konto=usuniete');
      },
      error: err => {
        this.deleting = false;
        this.deleteError = err?.error?.message || this.lang.t('Nie udało się usunąć konta.', 'Could not delete the account.');
      }
    });
  }

  grant() {
    const username = (this.grantUser || this.profile?.username || '').trim();
    if (!username) {
      return;
    }
    this.http.put<any>(environment.serverUrl + '/profile/' + encodeURIComponent(username) + '/rank', {title: this.grantTitle})
      .subscribe({
        next: profile => {
          this.grantUser = '';
          if (profile.username === this.profile?.username) {
            this.profile = profile;
            this.grantTitle = profile.granted ? profile.rank : '';
          }
          this.message = profile.granted ? 'Ranga przyznana.' : 'Ranga wróciła do aktywności.';
        },
        error: () => this.message = 'Nie udało się zmienić rangi.'
      });
  }

  selectFile(event) {
    this.selectedFile = event.target.files && event.target.files[0];
  }

  upload() {
    if (!this.selectedFile || !this.profile) {
      return;
    }
    this.uploadService.upload(this.selectedFile).subscribe({
      next: event => {
        if (event.type === HttpEventType.Response || event instanceof HttpResponse) {
          this.message = 'Zdjęcie zapisane.';
          this.getImageFromService();
        }
      },
      error: () => this.message = 'Nie udało się wgrać zdjęcia.'
    });
    this.selectedFile = null;
  }

  private load(username: string) {
    this.profile = null;
    this.trackPage = 1;
    this.missing = false;
    this.loading = true;
    this.imageToShow = null;
    this.message = '';
    const logged = this.token.getUser();
    this.self = !!this.token.getToken() && (!username || username === logged?.username);
    const url = username
      ? environment.serverUrl + '/profile/' + encodeURIComponent(username)
      : environment.serverUrl + '/profile/me';
    if (!username && !this.token.getToken()) {
      this.loading = false;
      return;
    }
    this.http.get(url).subscribe({
      next: (profile: any) => {
        this.loading = false;
        this.profile = profile;
        this.bio = profile.bio || '';
        this.firstName = profile.firstName || '';
        this.lastName = profile.lastName || '';
        this.website = profile.website || '';
        this.favorite = new Set((profile.favoriteGenres || '').split(',').map((item: string) => item.trim()).filter(Boolean));
        this.grantTitle = profile.granted ? profile.rank : '';
        this.self = !!logged && logged.username === profile.username;
        if (this.self) {
          this.autoplayNext = profile.autoplayNext !== false;
          this.playback.setAutoplayNext(this.autoplayNext);
        }
        this.getImageFromService();
      },
      error: () => {
        this.loading = false;
        this.missing = true;
      }
    });
  }

  /**
   * The picture is loaded by the browser straight from its URL. Reading the blob with a FileReader finished after
   * the post-HTTP view refresh, so the photo never appeared. The version query skips the 10-minute cache after an upload.
   */
  private getImageFromService() {
    if (!this.profile) {
      return;
    }
    this.imageToShow = environment.serverUrl + '/storage/' + encodeURIComponent(this.profile.username)
      + '/avatar?v=' + Date.now();
  }

  noAvatar() {
    this.imageToShow = null;
  }

}
