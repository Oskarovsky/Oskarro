import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {RefreshAfterHttpDirective} from '../services/ui-refresh';

/** Public "Zgłoś naruszenie" form; ?utwor=<id> pre-fills the reported track. */
@Component({
  selector: 'app-report',
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective],
  template: `
    <div class="page legal">
      <nav class="legal-nav" aria-label="Dokumenty">
        <a routerLink="/regulamin">Regulamin</a>
        <a routerLink="/polityka-prywatnosci">Polityka prywatności</a>
        <a routerLink="/cookies">Cookies</a>
        <a routerLink="/zglos-naruszenie" class="on">Zgłoś naruszenie</a>
      </nav>
      <p class="kicker">Moderacja</p>
      <h1 class="display">Zgłoś naruszenie</h1>
      <p class="lede">Utwór narusza Twoje prawa autorskie, ktoś spamuje, plik nie działa albo treść łamie regulamin? Napisz — każde zgłoszenie czyta administrator.</p>

      <div class="report-done" *ngIf="sent" role="status">
        <h2>Dziękujemy, zgłoszenie dotarło.</h2>
        <p>Rozpatrzymy je jak najszybciej{{ form.email ? ' i odpiszemy na ' + form.email : '' }}.</p>
        <a class="btn-ghost" routerLink="/">Wróć na stronę główną</a>
      </div>

      <form class="composer" *ngIf="!sent" (submit)="$event.preventDefault(); send()">
        <div class="form-grid">
          <label>Powód
            <select [(ngModel)]="form.reason" name="reason" required>
              <option value="COPYRIGHT">Naruszenie praw autorskich</option>
              <option value="SPAM">Spam lub reklama</option>
              <option value="OFFENSIVE">Treść obraźliwa lub bezprawna</option>
              <option value="BROKEN_LINK">Plik nie działa</option>
              <option value="OTHER">Inny powód</option>
            </select>
          </label>
          <label>Adres strony z treścią (opcjonalnie)
            <input [(ngModel)]="form.url" name="url" type="text" maxlength="300" placeholder="https://oskarro.com/track/…">
          </label>
          <label>Imię / nazwa firmy {{ form.reason === 'COPYRIGHT' ? '' : '(opcjonalnie)' }}
            <input [(ngModel)]="form.name" name="name" type="text" maxlength="100">
          </label>
          <label>E-mail do odpowiedzi {{ form.reason === 'COPYRIGHT' ? '(wymagany)' : '(opcjonalnie)' }}
            <input [(ngModel)]="form.email" name="email" type="email" maxlength="120">
          </label>
          <label class="wide">Opis
            <textarea [(ngModel)]="form.details" name="details" rows="6" maxlength="2000" required
                      [placeholder]="form.reason === 'COPYRIGHT' ? 'Jaki utwór, kto jest uprawniony i na jakiej podstawie (np. wytwórnia, autor).' : 'Co się stało?'"></textarea>
          </label>
          <label class="hp" aria-hidden="true">Strona www<input [(ngModel)]="form.website" name="website" tabindex="-1" autocomplete="off"></label>
        </div>
        <p class="meta" *ngIf="trackId">Zgłaszasz utwór nr {{ trackId }}.</p>
        <p class="meta">Dane ze zgłoszenia wykorzystamy tylko do jego rozpatrzenia — szczegóły w <a routerLink="/polityka-prywatnosci">polityce prywatności</a>.</p>
        <p class="alert" *ngIf="error" role="alert">{{ error }}</p>
        <button class="btn-accent" type="submit" [disabled]="sending">Wyślij zgłoszenie</button>
      </form>
    </div>
  `
})
export class ReportComponent implements OnInit {
  form = {reason: 'COPYRIGHT', url: '', name: '', email: '', details: '', website: ''};
  trackId: number | null = null;
  sending = false;
  sent = false;
  error = '';

  constructor(private route: ActivatedRoute, private http: HttpClient) {}

  ngOnInit() {
    const query = this.route.snapshot.queryParamMap;
    const id = Number(query.get('utwor'));
    if (Number.isInteger(id) && id > 0) {
      this.trackId = id;
      this.form.url = 'https://oskarro.com/track/' + id;
      this.form.reason = query.get('powod') === 'link' ? 'BROKEN_LINK' : 'COPYRIGHT';
    }
  }

  send() {
    this.error = '';
    if (this.form.details.trim().length < 10) {
      this.error = 'Opisz sprawę (co najmniej 10 znaków).';
      return;
    }
    if (this.form.reason === 'COPYRIGHT' && !this.form.email.trim()) {
      this.error = 'Przy naruszeniu praw autorskich podaj e-mail, żebyśmy mogli odpowiedzieć.';
      return;
    }
    this.sending = true;
    this.http.post(environment.serverUrl + '/reports', {...this.form, trackId: this.trackId}).subscribe({
      next: () => {
        this.sending = false;
        this.sent = true;
      },
      error: err => {
        this.sending = false;
        this.error = err?.error?.message || 'Nie udało się wysłać zgłoszenia. Spróbuj ponownie albo napisz e-mail.';
      }
    });
  }
}
