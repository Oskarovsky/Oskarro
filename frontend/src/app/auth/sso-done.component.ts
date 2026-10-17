import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {AuthService} from '../services/auth/auth.service';
import {TokenStorageService} from '../services/auth/token-storage.service';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';

const ERRORS: Record<string, string> = {
  'zablokowane': 'To konto jest zablokowane przez administratora.',
  'brak-email': 'Dostawca nie udostępnił adresu e-mail. Zezwól na dostęp do e-maila albo załóż konto zwykłym formularzem.',
  'niepotwierdzony-email': 'Adres e-mail w Google nie jest potwierdzony.',
  'anulowane': 'Logowanie zostało przerwane albo się nie udało. Spróbuj jeszcze raz.'
};

/** Landing page after Google/Facebook: the server puts our JWT (or an error code) in the URL fragment. */
@Component({
  selector: 'app-sso-done',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective],
  template: `
    <div class="auth-wrap">
      <div class="auth-card" style="padding: 24px;">
        <h1 class="display" style="font-size: 2rem; margin: 0 0 8px;">{{ error ? 'Nie udało się zalogować' : 'Logowanie…' }}</h1>
        <p class="lede" *ngIf="!error">Chwila, kończymy logowanie.</p>
        <p class="alert alert-danger" *ngIf="error" role="alert">{{ error }}</p>
        <a class="btn-ghost" *ngIf="error" routerLink="/login">Wróć do logowania</a>
      </div>
    </div>
  `
})
export class SsoDoneComponent implements OnInit {
  error = '';

  constructor(private auth: AuthService, private tokens: TokenStorageService, private router: Router) {}

  ngOnInit() {
    const fragment = new URLSearchParams(window.location.hash.replace(/^#/, ''));
    history.replaceState(null, '', window.location.pathname);
    const failure = fragment.get('blad');
    const token = fragment.get('token');
    if (failure || !token) {
      this.error = ERRORS[failure || 'anulowane'] || ERRORS['anulowane'];
      return;
    }
    this.auth.session(token).subscribe({
      next: data => {
        this.tokens.saveToken(data.accessToken);
        this.tokens.saveUser(data);
        window.location.assign('/');
      },
      error: () => this.error = ERRORS['anulowane']
    });
  }
}
