import {Component, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {ActivatedRoute} from '@angular/router';
import {environment} from '../../environments/environment';
import {LangService} from '../lang.service';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';

@Component({
  selector: 'app-activate',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective],
  template: `
    <div class="auth-wrap">
      <div class="auth-card">
        <h1 class="display" style="font-size: 2.2rem;">{{ 'Aktywacja konta' | t:'Account activation' }}</h1>
        <p class="lede" *ngIf="busy">{{ 'Sprawdzam link…' | t:'Checking the link…' }}</p>
        <p class="alert alert-success" *ngIf="ok">
          {{ 'Konto jest aktywne. Możesz się zalogować.' | t:'The account is active. You can sign in.' }}
          <a routerLink="/login">{{ 'Zaloguj się' | t:'Sign in' }}</a>
        </p>
        <p class="alert alert-danger" *ngIf="error">{{ error }}</p>
        <button class="btn btn-primary btn-block" type="button" *ngIf="retryable" (click)="activate()">{{ 'Spróbuj ponownie' | t:'Try again' }}</button>
      </div>
    </div>
  `
})
export class ActivateAccountComponent implements OnInit {
  busy = false;
  ok = false;
  error = '';
  retryable = false;
  private token = '';

  constructor(private http: HttpClient,
              private route: ActivatedRoute,
              private lang: LangService) {}

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
    if (!this.token) {
      this.error = this.lang.t('Link jest nieważny albo wygasł.', 'The link is invalid or it has expired.');
      return;
    }
    this.activate();
  }

  activate() {
    this.busy = true;
    this.error = '';
    this.retryable = false;
    this.http.post(environment.serverUrl + '/auth/activate', {token: this.token}).subscribe({
      next: () => {
        this.busy = false;
        this.ok = true;
      },
      error: err => {
        this.busy = false;
        // No answer or a server error (e.g. during a restart) says nothing about the link itself.
        this.retryable = !err?.status || err.status >= 500;
        this.error = this.retryable
          ? this.lang.t('Serwer chwilowo nie odpowiada. Link jest nadal ważny — spróbuj za chwilę.',
            'The server is not responding right now. The link is still valid — try again in a moment.')
          : this.lang.t('Link jest nieważny albo wygasł.', 'The link is invalid or it has expired.');
      }
    });
  }
}
