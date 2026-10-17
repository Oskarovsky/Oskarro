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
