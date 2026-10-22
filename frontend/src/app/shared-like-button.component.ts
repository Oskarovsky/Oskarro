import {Component, Input, OnChanges} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../environments/environment';
import {TokenStorageService} from './services/auth/token-storage.service';
import {LangService} from './lang.service';
import {RefreshAfterHttpDirective} from './services/ui-refresh';

interface LikeView {
  count: number;
  liked: boolean;
  downloadCredits: number;
}

/** "Lubię to" for playlists and video sets (tracks have their own +1/−1). */
@Component({
  selector: 'app-like-button',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective],
  template: `
    <span class="like-wrap">
      <button *ngIf="loggedIn" class="btn-ghost like-btn" [class.on]="liked" type="button" [disabled]="busy" (click)="toggle()"
              [attr.aria-pressed]="liked">
        {{ liked ? '♥' : '♡' }} {{ liked ? ('Lubisz to' | t:'You like this') : ('Lubię to' | t:'Like') }}
        <span class="like-count" *ngIf="count">{{ count }}</span>
      </button>
      <a *ngIf="!loggedIn" class="btn-ghost like-btn" routerLink="/login">♡ {{ 'Lubię to' | t:'Like' }}<span class="like-count" *ngIf="count">{{ count }}</span></a>
      <span class="meta" *ngIf="note" role="status">{{ note }}</span>
    </span>
  `
})
export class LikeButtonComponent implements OnChanges {
  @Input() kind: 'playlist' | 'video' = 'playlist';
  @Input() key = '';

  count = 0;
  liked = false;
  busy = false;
  note = '';
  private credits = 0;

  constructor(private http: HttpClient,
              private tokenStorage: TokenStorageService,
              private lang: LangService) {}

  get loggedIn() {
    return !!this.tokenStorage.getToken();
  }

  ngOnChanges() {
    if (!this.key) {
      return;
    }
    this.http.get<LikeView>(this.url()).subscribe({next: view => this.apply(view), error: () => {}});
  }

  toggle() {
    this.busy = true;
    this.note = '';
    const before = this.credits;
    this.http.post<LikeView>(this.url(), {}).subscribe({
      next: view => {
        this.busy = false;
        this.apply(view);
        if (view.liked && view.downloadCredits > before) {
          const n = view.downloadCredits;
          const pl = n === 1 ? 'żeton' : (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 12 || n % 100 > 14) ? 'żetony' : 'żetonów');
          this.note = this.lang.en ? `You now have ${n} token${n === 1 ? '' : 's'} for downloads.` : `Masz teraz ${n} ${pl} na pobrania.`;
        }
      },
      error: () => {
        this.busy = false;
        this.note = this.lang.t('Nie udało się zapisać. Spróbuj za chwilę.', 'Could not save it. Try again in a moment.');
      }
    });
  }

  private apply(view: LikeView) {
    this.count = view.count;
    this.liked = view.liked;
    this.credits = view.downloadCredits;
  }

  private url() {
    return environment.serverUrl + '/likes/' + this.kind + '/' + encodeURIComponent(this.key);
  }
}
