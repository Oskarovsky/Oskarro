import {Component, Input} from '@angular/core';

export function isHouseName(name: string): boolean {
  return (name || '').trim().toLowerCase() === 'oskarro';
}

@Component({
  selector: 'app-person',
  standalone: false,
  template: `
    <a *ngIf="to" class="person" [class.house]="house" [routerLink]="to">
      <ng-container *ngTemplateOutlet="mark"></ng-container>
    </a>
    <span *ngIf="!to" class="person" [class.house]="house">
      <ng-container *ngTemplateOutlet="mark"></ng-container>
    </span>
    <ng-template #mark>
      {{ name }}
      <img *ngIf="house" class="house-mark" src="assets/logo-mark.png" alt="" title="VIXA">
      <span *ngIf="!house && rank" class="rank-mark" [title]="rank">{{ rank | rankMark }}</span>
    </ng-template>
  `
})
export class PersonComponent {
  @Input() name = '';
  @Input() rank = '';
  @Input() to: any = null;

  get house() {
    return isHouseName(this.name);
  }
}
