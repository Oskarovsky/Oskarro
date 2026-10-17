import {Component, Input} from '@angular/core';
import {AdminEvent} from './admin.service';

const ICON: Record<string, string> = {user: '👤', track: '♫', comment: '💬', vote: '±', admin: '🛡'};

/** Timeline of site events (registrations, tracks, comments, votes, admin changes). */
@Component({
  selector: 'app-admin-feed',
  standalone: false,
  template: `
    <ul class="adm-feed" *ngIf="events?.length; else none">
      <li *ngFor="let e of events">
        <span class="icon" [ngClass]="e.type">{{ icon(e.type) }}</span>
        <span class="text">
          <a *ngIf="e.type !== 'admin'" [routerLink]="userLink(e)"><strong>{{ e.actor }}</strong></a>
          <strong *ngIf="e.type === 'admin'">{{ e.actor }}</strong>
          {{ e.text }}
          <a *ngIf="e.targetType === 'track' && e.targetId" [routerLink]="['/track', e.targetId]" class="adm-muted"> ↗</a>
        </span>
        <time [attr.title]="e.at">{{ e.at | ago }}</time>
      </li>
    </ul>
    <ng-template #none><p class="adm-empty">Brak zdarzeń.</p></ng-template>
  `
})
export class AdminFeedComponent {
  @Input() events: AdminEvent[] = [];

  icon(type: string) {
    return ICON[type] || '•';
  }

  userLink(e: AdminEvent) {
    return e.type === 'user' && e.targetId ? ['/admin/uzytkownicy', e.targetId] : ['/profile', e.actor];
  }
}
