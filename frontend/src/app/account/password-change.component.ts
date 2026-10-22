import {Component, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {TokenStorageService} from '../services/auth/token-storage.service';
import {LangService} from '../lang.service';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';

/** Its own page for changing the password, linked from the profile. */
@Component({
  selector: 'app-password-change',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective],
  template: `
    <div class="auth-wrap">
      <div class="auth-card">
        <header class="auth-head">
          <h1 class="display">{{ 'Zmień hasło' | t:'Change password' }}</h1>
          <p class="lede" *ngIf="!external">{{ 'Podaj obecne hasło i dwa razy nowe.' | t:'Enter your current password and the new one twice.' }}</p>
        </header>

        <p class="meta auth-links" *ngIf="!loggedIn"><a routerLink="/login">{{ 'Zaloguj się' | t:'Sign in' }}</a>{{ lang.en ? ' to change your password.' : ', żeby zmienić hasło.' }}</p>

        <div class="alert alert-success auth-done" role="status" *ngIf="loggedIn && external">
          {{ lang.en
            ? 'You sign in with ' + providerName + ', so your account has no password of its own. If you want one, use "Forgot your password?" on the sign-in page.'
            : 'Logujesz się przez ' + providerName + ', więc konto nie ma własnego hasła. Jeśli chcesz je mieć, użyj „Nie pamiętasz hasła?” na stronie logowania.' }}
        </div>

        <form class="auth-form" *ngIf="loggedIn && !external && !done" (ngSubmit)="save()" novalidate>
          <div class="form-group">
            <label for="pw-current">{{ 'Obecne hasło' | t:'Current password' }}</label>
            <div class="password-field">
              <input id="pw-current" class="form-control" name="current" [type]="show ? 'text' : 'password'" [(ngModel)]="current" autocomplete="current-password" required>
              <button type="button" class="password-toggle" (click)="show = !show" [attr.aria-pressed]="show">{{ show ? ('Ukryj' | t:'Hide') : ('Pokaż' | t:'Show') }}</button>
            </div>
          </div>
          <div class="form-group">
            <label for="pw-next">{{ 'Nowe hasło' | t:'New password' }}</label>
            <input id="pw-next" class="form-control" name="next" [type]="show ? 'text' : 'password'" [(ngModel)]="next" autocomplete="new-password" minlength="6" maxlength="40" required>
            <p class="field-hint">{{ 'Od 6 do 40 znaków.' | t:'6 to 40 characters.' }}</p>
          </div>
          <div class="form-group">
            <label for="pw-confirm">{{ 'Powtórz nowe hasło' | t:'Repeat the new password' }}</label>
            <input id="pw-confirm" class="form-control" name="confirm" [type]="show ? 'text' : 'password'" [(ngModel)]="confirm" autocomplete="new-password" required>
          </div>
          <button class="btn btn-primary btn-block auth-submit" type="submit" [disabled]="busy">
            {{ busy ? ('Zapisuję…' | t:'Saving…') : ('Zmień hasło' | t:'Change password') }}
          </button>
          <div class="alert alert-danger auth-error" role="alert" *ngIf="error">{{ error }}</div>
        </form>

        <div class="alert alert-success auth-done" role="status" *ngIf="done">{{ 'Hasło zmienione. Następnym razem zaloguj się nowym.' | t:'Password changed. Use the new one next time you sign in.' }}</div>
        <p class="meta auth-links" *ngIf="loggedIn"><a routerLink="/profile">← {{ 'Wróć do profilu' | t:'Back to your profile' }}</a></p>
      </div>
    </div>
  `
})
export class PasswordChangeComponent implements OnInit {
  current = '';
  next = '';
  confirm = '';
  show = false;
  busy = false;
  done = false;
  error = '';
  external = false;
  provider = '';

  constructor(private http: HttpClient,
              private tokenStorage: TokenStorageService,
              readonly lang: LangService) {}

  get loggedIn() {
    return !!this.tokenStorage.getToken();
  }

  get providerName() {
    return this.provider === 'facebook' ? 'Facebook' : 'Google';
  }

  ngOnInit() {
    if (!this.loggedIn) {
      return;
    }
    this.http.get<any>(environment.serverUrl + '/profile/me').subscribe({
      next: profile => {
        this.provider = profile?.signInMethod || 'password';
        this.external = this.provider !== 'password';
      },
      error: () => {}
    });
  }

  save() {
    this.error = '';
    if (!this.current) {
      this.error = this.lang.t('Podaj obecne hasło.', 'Enter your current password.');
      return;
    }
    if (this.next.length < 6 || this.next.length > 40) {
      this.error = this.lang.t('Nowe hasło musi mieć od 6 do 40 znaków.', 'The new password must be 6 to 40 characters.');
      return;
    }
    if (this.next !== this.confirm) {
      this.error = this.lang.t('Nowe hasła nie są takie same.', 'The new passwords do not match.');
      return;
    }
    this.busy = true;
    this.http.post(environment.serverUrl + '/profile/password', {current: this.current, next: this.next}).subscribe({
      next: () => {
        this.busy = false;
        this.done = true;
        this.current = this.next = this.confirm = '';
      },
      error: err => {
        this.busy = false;
        const raw = err?.error?.message || '';
        this.error = raw.includes('Obecne hasło')
          ? this.lang.t('Obecne hasło się nie zgadza.', 'The current password is wrong.')
          : (raw && !this.lang.en ? raw : this.lang.t('Nie udało się zmienić hasła. Spróbuj za chwilę.', 'Could not change the password. Try again in a moment.'));
      }
    });
  }
}
