import {Component, Input} from '@angular/core';

/**
 * Hand-drawn SVG artwork for each genre tile on the home page.
 * Every genre gets its own palette and motif, so the tiles read as distinct "rooms".
 */
@Component({
  selector: 'app-genre-art',
  standalone: false,
  template: `
    <svg viewBox="0 0 320 180" preserveAspectRatio="xMidYMid slice" aria-hidden="true" [ngSwitch]="id">
      <defs>
        <linearGradient [attr.id]="'g-' + id + key" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" [attr.stop-color]="palette[0]"/>
          <stop offset="1" [attr.stop-color]="palette[1]"/>
        </linearGradient>
        <radialGradient [attr.id]="'r-' + id + key" cx="0.5" cy="0.5" r="0.5">
          <stop offset="0" stop-color="#fff" stop-opacity="0.85"/>
          <stop offset="1" stop-color="#fff" stop-opacity="0"/>
        </radialGradient>
      </defs>
      <rect width="320" height="180" [attr.fill]="'url(#g-' + id + key + ')'"/>

      <!-- Club: laser fan from the booth -->
      <g *ngSwitchCase="'CLUB'" stroke="#fff" stroke-linecap="round">
        <line *ngFor="let a of fan" x1="160" y1="190" [attr.x2]="160 + a * 26" y2="-10" stroke-width="1.6" [attr.stroke-opacity]="0.25 + (a % 2 ? 0.35 : 0.1)"/>
        <circle cx="160" cy="184" r="34" [attr.fill]="'url(#r-' + id + key + ')'" stroke="none" opacity="0.7"/>
      </g>

      <!-- Dance: speaker pulse -->
      <g *ngSwitchCase="'DANCE'" fill="none" stroke="#fff">
        <circle *ngFor="let r of rings" cx="240" cy="90" [attr.r]="r" stroke-width="2" [attr.stroke-opacity]="1 - r / 150"/>
        <circle cx="240" cy="90" r="14" fill="#fff" fill-opacity="0.9" stroke="none"/>
      </g>

      <!-- Retro: sunset with stripes and a record -->
      <g *ngSwitchCase="'RETRO'">
        <circle cx="210" cy="118" r="70" fill="#ffe27a" opacity="0.95"/>
        <rect *ngFor="let y of stripes" x="130" [attr.y]="y" width="170" [attr.height]="4 + (y - 100) / 10" [attr.fill]="palette[0]"/>
        <circle cx="62" cy="52" r="40" fill="#1a1410"/>
        <circle cx="62" cy="52" r="30" fill="none" stroke="#fff" stroke-opacity="0.15"/>
        <circle cx="62" cy="52" r="21" fill="none" stroke="#fff" stroke-opacity="0.15"/>
        <circle cx="62" cy="52" r="10" [attr.fill]="palette[1]"/>
      </g>

      <!-- House: equaliser -->
      <g *ngSwitchCase="'HOUSE'" fill="#fff">
        <rect *ngFor="let h of eq; let i = index" [attr.x]="22 + i * 24" [attr.y]="168 - h" width="14" [attr.height]="h" rx="4" [attr.fill-opacity]="0.35 + (i % 3) * 0.2"/>
      </g>

      <!-- Vixa: neon bolts -->
      <g *ngSwitchCase="'VIXA'" fill="none" stroke-linejoin="round" stroke-linecap="round">
        <path d="M150 10 L120 92 L162 92 L132 172" stroke="#fff" stroke-width="10"/>
        <path d="M150 10 L120 92 L162 92 L132 172" stroke="#00f0ff" stroke-width="4"/>
        <path d="M250 30 L232 86 L258 86 L240 140" stroke="#fff" stroke-width="6" stroke-opacity="0.7"/>
        <path d="M60 40 L46 84 L66 84 L52 128" stroke="#fff" stroke-width="5" stroke-opacity="0.5"/>
      </g>

      <!-- Bass house: heavy sub wave -->
      <g *ngSwitchCase="'BASS_HOUSE'" fill="none" stroke="#fff" stroke-linecap="round">
        <path d="M-10 100 C 30 20, 70 180, 110 100 S 190 20, 230 100 S 310 180, 340 100" stroke-width="10" stroke-opacity="0.9"/>
        <path d="M-10 118 C 30 60, 70 176, 110 118 S 190 60, 230 118 S 310 176, 340 118" stroke-width="3" stroke-opacity="0.45"/>
        <path d="M-10 80 C 30 24, 70 140, 110 80 S 190 24, 230 80 S 310 140, 340 80" stroke-width="2" stroke-opacity="0.3"/>
      </g>

      <!-- Techno: perspective floor and strobe -->
      <g *ngSwitchCase="'TECHNO'" stroke="#fff" fill="none">
        <line *ngFor="let x of floor" x1="160" y1="70" [attr.x2]="x" y2="190" stroke-opacity="0.35"/>
        <line *ngFor="let y of floorRows" x1="0" [attr.y1]="y" x2="320" [attr.y2]="y" stroke-opacity="0.3"/>
        <rect x="0" y="0" width="320" height="70" fill="#000" fill-opacity="0.35" stroke="none"/>
        <circle cx="160" cy="58" r="40" [attr.fill]="'url(#r-' + id + key + ')'" stroke="none"/>
      </g>

      <!-- Big room: stage light cones over the crowd -->
      <g *ngSwitchCase="'BIG_ROOM'">
        <polygon *ngFor="let x of cones" [attr.points]="x + ',0 ' + (x - 60) + ',180 ' + (x + 60) + ',180'" fill="#fff" fill-opacity="0.13"/>
        <path d="M0 180 Q 10 150 20 170 Q 30 142 42 168 Q 54 146 64 170 Q 76 140 88 168 Q 100 150 112 172 Q 124 138 136 168 Q 148 148 160 170 Q 172 140 184 168 Q 196 150 208 172 Q 220 142 232 168 Q 244 146 256 170 Q 268 140 280 168 Q 292 150 304 170 Q 312 152 320 166 L320 180 Z" fill="#120d1c"/>
      </g>

      <!-- Trance: layered flowing waves -->
      <g *ngSwitchCase="'TRANCE'" fill="none" stroke="#fff" stroke-linecap="round">
        <path *ngFor="let o of waves" [attr.d]="'M-10 ' + (60 + o) + ' C 70 ' + (10 + o) + ', 150 ' + (130 + o) + ', 230 ' + (70 + o) + ' S 330 ' + (30 + o) + ', 340 ' + (60 + o)" stroke-width="2" [attr.stroke-opacity]="0.2 + o / 100"/>
      </g>

      <!-- Electro: circuit traces -->
      <g *ngSwitchCase="'ELECTRO'" fill="none" stroke="#fff" stroke-width="2.5" stroke-linejoin="round">
        <path d="M0 40 H90 L120 70 H220 L250 40 H320" stroke-opacity="0.6"/>
        <path d="M0 120 H60 L90 150 H180 L210 110 H320" stroke-opacity="0.45"/>
        <path d="M150 0 V50 L170 70 V180" stroke-opacity="0.35"/>
        <circle cx="120" cy="70" r="6" fill="#fff"/>
        <circle cx="210" cy="110" r="6" fill="#fff"/>
        <circle cx="170" cy="70" r="5" fill="#fff" fill-opacity="0.7"/>
        <circle cx="90" cy="150" r="5" fill="#fff" fill-opacity="0.7"/>
      </g>

      <!-- Disco polo: mirror ball and sparkles -->
      <g *ngSwitchCase="'DISCO_POLO'">
        <line x1="210" y1="0" x2="210" y2="34" stroke="#fff" stroke-opacity="0.6"/>
        <circle cx="210" cy="86" r="52" fill="#d9d4ff" fill-opacity="0.25" stroke="#fff" stroke-opacity="0.6"/>
        <g stroke="#fff" stroke-opacity="0.45">
          <ellipse cx="210" cy="86" rx="52" ry="18" fill="none"/>
          <ellipse cx="210" cy="86" rx="52" ry="36" fill="none"/>
          <ellipse cx="210" cy="86" rx="18" ry="52" fill="none"/>
          <ellipse cx="210" cy="86" rx="36" ry="52" fill="none"/>
          <line x1="158" y1="86" x2="262" y2="86"/>
        </g>
        <circle cx="192" cy="66" r="10" [attr.fill]="'url(#r-' + id + key + ')'"/>
        <path *ngFor="let s of sparkles" [attr.d]="'M' + s[0] + ' ' + (s[1] - 8) + ' L' + (s[0] + 2) + ' ' + (s[1] - 2) + ' L' + (s[0] + 8) + ' ' + s[1] + ' L' + (s[0] + 2) + ' ' + (s[1] + 2) + ' L' + s[0] + ' ' + (s[1] + 8) + ' L' + (s[0] - 2) + ' ' + (s[1] + 2) + ' L' + (s[0] - 8) + ' ' + s[1] + ' L' + (s[0] - 2) + ' ' + (s[1] - 2) + ' Z'" fill="#fff"/>
      </g>


      <!-- Hands up: raised hands and a melodic lead line -->
      <g *ngSwitchCase="'HANDS_UP'">
        <path d="M-10 70 Q 40 20, 80 60 T 170 50 T 260 40 T 340 30" fill="none" stroke="#fff" stroke-width="3" stroke-opacity="0.75" stroke-linecap="round"/>
        <g fill="#0b2a2a" fill-opacity="0.85">
          <path *ngFor="let x of hands; let i = index" [attr.d]="'M' + x + ' 180 L' + x + ' ' + (118 - (i % 3) * 12) + ' q 0 -10 6 -10 q 6 0 6 10 L' + (x + 12) + ' 180 Z'"/>
          <ellipse *ngFor="let x of hands; let i = index" [attr.cx]="x + 6" [attr.cy]="(108 - (i % 3) * 12)" rx="9" ry="11"/>
        </g>
        <circle cx="250" cy="44" r="26" [attr.fill]="'url(#r-' + id + key + ')'"/>
      </g>

      <!-- Pop: bubbles and a star burst -->
      <g *ngSwitchCase="'POP'">
        <circle *ngFor="let b of bubbles" [attr.cx]="b[0]" [attr.cy]="b[1]" [attr.r]="b[2]" fill="#fff" [attr.fill-opacity]="b[3]"/>
        <path d="M230 30 L242 72 L286 72 L250 98 L264 140 L230 114 L196 140 L210 98 L174 72 L218 72 Z" fill="#fff" fill-opacity="0.9"/>
      </g>
    </svg>
  `,
  styles: [`
    :host { display: block; width: 100%; height: 100%; }
    svg { display: block; width: 100%; height: 100%; }
  `]
})
export class GenreArtComponent {
  @Input() id = 'CLUB';
  /** Optional palette override (e.g. set categories reuse a motif in their own colours). */
  @Input() colors: [string, string] | null = null;
  /** Unique gradient ids when the same motif appears twice on one page. */
  @Input() key = '';

  readonly fan = [-5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5];
  readonly rings = [24, 40, 58, 78, 100, 124, 150];
  readonly stripes = [104, 116, 128, 140, 152, 164];
  readonly eq = [40, 72, 110, 86, 130, 98, 60, 118, 146, 92, 66, 104, 50];
  readonly floor = [-200, -100, 0, 60, 120, 160, 200, 260, 320, 420, 520];
  readonly floorRows = [86, 106, 132, 164];
  readonly cones = [60, 160, 260];
  readonly waves = [0, 14, 28, 42, 56, 70];
  readonly hands = [20, 52, 84, 116, 148, 180, 212, 244, 276];
  readonly bubbles = [[40, 50, 22, 0.25], [90, 120, 34, 0.18], [150, 40, 14, 0.35], [300, 150, 30, 0.2], [120, 160, 10, 0.4], [60, 150, 8, 0.5]];
  readonly sparkles = [[60, 40], [110, 120], [280, 150], [40, 140], [290, 36]];

  private static readonly PALETTES: Record<string, [string, string]> = {
    CLUB: ['#3b1a78', '#c2187a'],
    DANCE: ['#ff3d7f', '#ff9a3d'],
    RETRO: ['#ff5f6d', '#ffc371'],
    HOUSE: ['#0f6b74', '#2f80ed'],
    VIXA: ['#ff00a8', '#5b2bff'],
    BASS_HOUSE: ['#141e5a', '#7b2ff7'],
    TECHNO: ['#1b1b1f', '#8c1c1c'],
    BIG_ROOM: ['#2a1b5c', '#f2a03d'],
    TRANCE: ['#0bb4d8', '#6a3cff'],
    ELECTRO: ['#0d7a4f', '#18c6b3'],
    DISCO_POLO: ['#c8102e', '#f5b700'],
    HANDS_UP: ['#00c6a7', '#1e3c72'],
    POP: ['#ff6ec4', '#7873f5']
  };

  get palette(): [string, string] {
    if (this.colors) {
      return this.colors;
    }
    return GenreArtComponent.PALETTES[this.id] || GenreArtComponent.PALETTES['CLUB'];
  }
}
