import {Component, Input} from '@angular/core';
import {environment} from '../../environments/environment';

const COLORS = ['#e4ff4a', '#ff8d7a', '#7ad7ff', '#c79bff', '#ffc15e', '#6fe3a5'];

/** A user's picture, or their first letter on a colour picked from the name when there is none. */
@Component({
  selector: 'app-avatar',
  standalone: false,
  template: `
    <img *ngIf="has && !failed" class="avatar-mini" [style.width.px]="size" [style.height.px]="size"
         [src]="src" alt="" loading="lazy" (error)="failed = true">
    <span *ngIf="!has || failed" class="avatar-mini avatar-letter" aria-hidden="true"
          [style.width.px]="size" [style.height.px]="size" [style.font-size.px]="size * 0.45" [style.background]="color">{{ letter }}</span>
  `
})
export class AvatarComponent {
  @Input() name = '';
  @Input() has = false;
  @Input() size = 40;
  failed = false;

  get src() {
    return environment.serverUrl + '/storage/' + encodeURIComponent(this.name) + '/avatar';
  }

  get letter() {
    return (this.name || '?').trim().charAt(0).toUpperCase();
  }

  get color() {
    let hash = 0;
    for (const ch of this.name || '') {
      hash = (hash * 31 + ch.charCodeAt(0)) | 0;
    }
    return COLORS[Math.abs(hash) % COLORS.length];
  }
}
