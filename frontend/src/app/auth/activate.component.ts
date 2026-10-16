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
      </div>
    </div>
  `
})
export class ActivateAccountComponent implements OnInit {
  busy = false;
  ok = false;
  error = '';

  constructor(private http: HttpClient,
              private route: ActivatedRoute,
              private lang: LangService) {}

  ngOnInit() {
    const token = this.route.snapshot.queryParamMap.get('token') || '';
    if (!token) {
      this.error = this.lang.t('Link jest nieważny albo wygasł.', 'The link is invalid or it has expired.');
      return;
    }
    this.busy = true;
    this.http.post(environment.serverUrl + '/auth/activate', {token}).subscribe({
      next: () => {
        this.busy = false;
        this.ok = true;
      },
      error: () => {
        this.busy = false;
        this.error = this.lang.t('Link jest nieważny albo wygasł.', 'The link is invalid or it has expired.');
      }
    });
  }
}
