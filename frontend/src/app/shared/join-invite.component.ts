import {Component, Input} from '@angular/core';

/** Friendly invitation for guests who hit a limit: why an account is worth it and how quick it is. */
@Component({
  selector: 'app-join-invite',
  standalone: false,
  template: `
    <div class="join-invite" role="note">
      <p class="join-title">{{ headline || ('Słuchaj dalej za darmo' | t:'Keep listening for free') }}</p>
      <p class="meta">{{ 'Załóż konto w 10 sekund — przez Google, Facebooka albo e-mail — i odbierz 3 żetony na pobieranie plików na start.' | t:'Create an account in 10 seconds — with Google, Facebook or e-mail — and get 3 download tokens to start with.' }}</p>
      <ul class="join-perks">
        <li>{{ 'radio i całe składanki bez limitu' | t:'radio and whole playlists without limits' }}</li>
        <li>{{ 'ocenianie, komentarze, własne składanki' | t:'voting, comments, your own playlists' }}</li>
        <li>{{ 'pobieranie plików za żetony' | t:'file downloads with tokens' }}</li>
      </ul>
      <div class="join-actions">
        <a class="btn-accent" routerLink="/register">{{ 'Załóż konto za darmo' | t:'Join for free' }}</a>
        <a class="btn-ghost" routerLink="/login">{{ 'Mam już konto' | t:'I have an account' }}</a>
      </div>
    </div>
  `
})
export class JoinInviteComponent {
  @Input() headline = '';
}
