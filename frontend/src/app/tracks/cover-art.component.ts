import {Component, Input} from '@angular/core';
import {genreLabel} from './genres';
import {LangService} from '../lang.service';

const WASH: Record<string, string> = {
  CLUB: '#e4ff4a',
  DANCE: '#5ee7ff',
  RETRO: '#ffb15a',
  HOUSE: '#d9a6ff',
  VIXA: '#ff6d9a',
  BASS_HOUSE: '#7dffb0',
  TECHNO: '#8d97ff',
  BIG_ROOM: '#ff8a3d',
  TRANCE: '#7af0ff',
  ELECTRO: '#ff4ad2',
  DISCO_POLO: '#ffe14a'
};

let nextCover = 0;

@Component({
  selector: 'app-cover',
  standalone: false,
  template: `
    <img *ngIf="hasCover && src" class="art" [src]="src" alt="">
    <svg *ngIf="!hasCover || !src" class="art" viewBox="0 0 480 480" preserveAspectRatio="xMidYMid slice" role="img" [attr.aria-label]="coverLabel">
      <defs>
        <linearGradient [attr.id]="grad" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" [attr.stop-color]="wash" stop-opacity="0.9"/>
          <stop offset="0.42" [attr.stop-color]="wash" stop-opacity="0.18"/>
          <stop offset="1" stop-color="#12110e"/>
        </linearGradient>
      </defs>
      <rect width="480" height="480" fill="#16140f"/>
      <rect width="480" height="480" [attr.fill]="'url(#' + grad + ')'"/>
      <rect x="18" y="18" width="444" height="444" fill="none" stroke="rgba(246,241,230,0.18)" stroke-width="2"/>
      <image href="assets/logo-mark.png" x="36" y="34" width="72" height="72"/>
      <text x="124" y="78" fill="#f6f1e6" font-family="Outfit, sans-serif" font-size="28" font-weight="650">oskarro.com</text>
      <text x="36" y="210" [attr.fill]="wash" font-family="Fraunces, Georgia, serif" [attr.font-size]="titleSize">{{ label }}</text>
      <rect *ngFor="let bar of bars; let i = index" [attr.x]="36 + i * 54" [attr.y]="400 - bar" width="28" [attr.height]="bar" [attr.fill]="wash" opacity="0.9" rx="4"/>
    </svg>
  `
})
export class CoverArtComponent {
  constructor(private lang: LangService) {}

  @Input() hasCover = false;
  @Input() src = '';
  @Input() genre = '';

  grad = 'cover-wash-' + (++nextCover);

  get genreId() {
    return (this.genre || 'CLUB').trim().toUpperCase().replace(/[-\s]+/g, '_');
  }

  get coverLabel() {
    return this.lang.t('Domyślna okładka Oskarro, ', 'Default Oskarro cover, ') + this.label;
  }

  get label() {
    return genreLabel(this.genreId);
  }

  get titleSize() {
    return this.label.length > 7 ? 46 : 58;
  }

  get wash() {
    return WASH[this.genreId] || '#e4ff4a';
  }

  get bars() {
    const key = this.genreId;
    return Array.from({length: 8}, (_, index) => {
      const code = key.charCodeAt(index % key.length) || 40;
      return 36 + ((code * (index + 3)) % 110);
    });
  }
}
