import {Component, Input} from '@angular/core';
import {LangService} from '../lang.service';

@Component({
  selector: 'app-track-facts',
  standalone: false,
  template: `
    <ng-container *ngIf="part === 'all'">
      <span *ngIf="viewsCount != null"> · {{ viewsPhrase }}</span>
      <span *ngIf="commentTotal != null"> · {{ commentsPhrase }}</span>
      <span *ngIf="track?.createdAt"> · {{ track.createdAt | date:'dd.MM.yyyy HH:mm':'Europe/Warsaw' }}</span>
    </ng-container>
    <ng-container *ngIf="part === 'stats'">
      <span class="fact">{{ viewsPhrase }}</span>
      <span class="fact">{{ commentsPhrase }}</span>
    </ng-container>
  `
})
export class TrackFactsComponent {
  @Input() track: any;
  /** 'all' = one inline run (detail pages); 'stats' = views and comments only (list rows). */
  @Input() part: 'all' | 'stats' = 'all';

  constructor(private lang: LangService) {}

  get viewsCount(): number | null {
    const value = this.track?.viewsAll;
    return value == null || value === '' ? null : Number(value);
  }

  get commentTotal(): number | null {
    const value = this.track?.commentCount;
    return value == null || value === '' ? null : Number(value);
  }

  get viewsPhrase() {
    return (this.viewsCount || 0) + ' ' + this.lang.plural(this.viewsCount || 0,
      ['wyświetlenie', 'wyświetlenia', 'wyświetleń'], ['view', 'views']);
  }

  get commentsPhrase() {
    return (this.commentTotal || 0) + ' ' + this.lang.plural(this.commentTotal || 0,
      ['komentarz', 'komentarze', 'komentarzy'], ['comment', 'comments']);
  }
}
