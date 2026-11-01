import {Component, Input} from '@angular/core';
import {TrackService} from '../services/track/track.service';

/** Track rows shared by the "Nowości" and artist pages (same look as the genre list). */
@Component({
  selector: 'app-explore-tracks',
  standalone: false,
  template: `
    <div class="latest-grid" *ngIf="tracks?.length">
      <a class="track-row" *ngFor="let track of tracks" [routerLink]="['/track', track.id]">
        <span class="thumb-col">
          <span class="thumb-slot">
            <app-cover [hasCover]="track.hasCover" [src]="trackService.coverUrl(track.id)" [genre]="track.genre"></app-cover>
          </span>
          <app-score [value]="track.score"></app-score>
        </span>
        <span class="track-lines">
          <h2>{{ track.title }}<span class="version" *ngIf="track | versionLabel as version"> ({{ version }})</span></h2>
          <span class="line artist">{{ track.artist }}</span>
          <span class="line meta"><app-track-facts [track]="track" part="stats"></app-track-facts></span>
          <span class="line meta added-by" *ngIf="track.createdAt">{{ track.createdAt | date:'dd.MM.yyyy':'Europe/Warsaw' }}</span>
        </span>
      </a>
    </div>
  `
})
export class ExploreTracksComponent {
  @Input() tracks: any[] = [];

  constructor(readonly trackService: TrackService) {}
}
