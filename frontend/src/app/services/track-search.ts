import { Injectable, signal } from '@angular/core';

@Injectable({providedIn: 'root'})
export class TrackSearch {
  private readonly query = signal('');

  get text() {
    return this.query();
  }

  set text(value: string) {
    this.query.set(value ?? '');
  }
}

export function trackMatches(track, query: string): boolean {
  const parts = (query || '').trim().toLowerCase().split(/\s+/).filter(Boolean);
  if (!parts.length) {
    return true;
  }
  const hay = [track?.title, track?.artist, track?.version, track?.sharedBy, track?.genre]
    .filter(Boolean)
    .join(' ')
    .toLowerCase();
  return parts.every(part => hay.includes(part));
}
