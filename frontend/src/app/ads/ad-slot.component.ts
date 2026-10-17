import {Component, Input} from '@angular/core';
import {LangService} from '../lang.service';

/** Placeholders stay hidden until a real ad network is connected — empty "Ad space" boxes look unfinished. */
export const ADS_ENABLED = false;

@Component({
  selector: 'app-ad-slot',
  standalone: false,
  template: `
    <aside *ngIf="enabled" class="ad-slot" [class.box]="format === 'box'" [attr.data-ad-slot]="slot" [attr.data-ad-format]="format" [attr.aria-label]="'Reklama' | t:'Ad'">
      <span class="ad-kicker">{{ 'Reklama' | t:'Ad' }}</span>
      <span class="ad-copy">{{ 'Miejsce na reklamę' | t:'Ad space' }}</span>
      <span class="ad-size">{{ sizeLabel }}</span>
    </aside>
  `
})
export class AdSlotComponent {
  @Input() slot = 'baner';
  @Input() format: 'banner' | 'box' = 'banner';
  readonly enabled = ADS_ENABLED;

  constructor(private lang: LangService) {}

  get sizeLabel(): string {
    return this.format === 'box'
      ? this.lang.t('Pole 300 × 250', 'Box 300 × 250')
      : this.lang.t('Baner 970 × 90', 'Banner 970 × 90');
  }
}
