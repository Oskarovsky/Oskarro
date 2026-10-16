import {Component, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {ActivatedRoute} from '@angular/router';
import {environment} from '../../environments/environment';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';

@Component({
  selector: 'app-password-reset',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective],
  template: `
    <div class="auth-wrap">
      <div class="auth-card">
        <ng-container *ngIf="mode === 'forgot'">
          <h1 class="display" style="font-size: 2.2rem;">{{ 'Nie pamiętasz hasła?' | t:'Forgot your password?' }}</h1>
          <p class="lede">{{ 'Podaj email konta. Wyślemy link do nowego hasła.' | t:'Enter the account email. We will send a link for a new password.' }}</p>
          <form *ngIf="!sent" (submit)="$event.preventDefault(); ask()">
            <div class="form-group">
              <label for="reset-email">Email</label>
              <input id="reset-email" type="email" name="email" [(ngModel)]="email" required autocomplete="email">
            </div>
            <button class="btn btn-primary" type="submit" [disabled]="busy || !email.includes('@')">{{ 'Wyślij link' | t:'Send the link' }}</button>
          </form>
          <p class="meta" *ngIf="sent && !delivered">{{ 'Jeśli konto z tym adresem istnieje, link przyjdzie na maila. Gdy skrzynka nie jest podłączona, wiadomość nie wyjdzie.' | t:'If an account with this address exists, the link will arrive by email. If the mailbox is not connected, the message will not go out.' }}</p>
          <p class="meta" *ngIf="sent && delivered">{{ 'Sprawdź skrzynkę. Link działa dwie godziny.' | t:'Check your inbox. The link works for two hours.' }}</p>
        </ng-container>

        <ng-container *ngIf="mode === 'reset'">
          <h1 class="display" style="font-size: 2.2rem;">{{ 'Nowe hasło' | t:'New password' }}</h1>
          <form *ngIf="!done" (submit)="$event.preventDefault(); save()">
            <div class="form-group">
              <label for="new-pass">{{ 'Nowe hasło' | t:'New password' }}</label>
              <input id="new-pass" type="password" name="password" [(ngModel)]="password" required minlength="6" autocomplete="new-password">
            </div>
            <div class="form-group">
              <label for="new-pass-2">{{ 'Powtórz hasło' | t:'Repeat the password' }}</label>
              <input id="new-pass-2" type="password" name="confirm" [(ngModel)]="confirm" required autocomplete="new-password">
            </div>
            <p class="alert alert-danger" *ngIf="error">{{ error }}</p>
            <button class="btn btn-primary" type="submit" [disabled]="busy || password.length < 6 || password !== confirm">{{ 'Zapisz hasło' | t:'Save password' }}</button>
          </form>
          <p class="meta" *ngIf="done">{{ 'Hasło zapisane. Możesz się zalogować.' | t:'Password saved. You can sign in.' }} <a routerLink="/login">{{ 'Zaloguj się' | t:'Sign in' }}</a></p>
        </ng-container>
      </div>
    </div>
  `
})
export class PasswordResetComponent implements OnInit {
  mode: 'forgot' | 'reset' = 'forgot';
  email = '';
  password = '';
  confirm = '';
  token = '';
  busy = false;
  sent = false;
  delivered = false;
  done = false;
  error = '';

  constructor(private http: HttpClient, private route: ActivatedRoute) {}

  ngOnInit() {
    this.mode = this.route.snapshot.data['mode'] === 'reset' ? 'reset' : 'forgot';
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
  }

  ask() {
    this.busy = true;
    this.http.post<{ok: boolean, delivered: boolean}>(environment.serverUrl + '/auth/forgot', {email: this.email.trim()})
      .subscribe({
        next: result => {
          this.busy = false;
          this.sent = true;
          this.delivered = !!result?.delivered;
        },
        error: () => {
          this.busy = false;
          this.sent = true;
          this.delivered = false;
        }
      });
  }

  save() {
    this.error = '';
    if (!this.token) {
      this.error = 'Link jest nieważny.';
      return;
    }
    this.busy = true;
    this.http.post(environment.serverUrl + '/auth/reset', {token: this.token, password: this.password}).subscribe({
      next: () => {
        this.busy = false;
        this.done = true;
      },
      error: () => {
        this.busy = false;
        this.error = 'Link jest nieważny albo wygasł.';
      }
    });
  }
}
