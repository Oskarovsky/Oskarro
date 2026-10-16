import {Component, EventEmitter, Input, Output} from '@angular/core';

@Component({
  selector: 'app-pager',
  standalone: false,
  template: `
    <nav class="pager" *ngIf="count > 1" [attr.aria-label]="label | t">
      <button class="btn-ghost" type="button" (click)="move(page - 1)" [disabled]="page <= 1">{{ 'Poprzednia' | t:'Previous' }}</button>
      <button class="btn-ghost" type="button" *ngFor="let n of numbers" [class.on]="n === page" (click)="move(n)" [attr.aria-current]="n === page ? 'page' : null">{{ n }}</button>
      <button class="btn-ghost" type="button" (click)="move(page + 1)" [disabled]="page >= count">{{ 'Następna' | t:'Next' }}</button>
    </nav>
  `
})
export class PagerComponent {
  @Input() page = 1;
  @Input() count = 1;
  @Input() label = 'Strony utworów';
  @Output() pageChange = new EventEmitter<number>();

  get numbers(): number[] {
    const total = Math.max(0, this.count);
    return Array.from({length: total}, (_, index) => index + 1);
  }

  move(next: number) {
    if (next < 1 || next > this.count || next === this.page) {
      return;
    }
    this.pageChange.emit(next);
  }
}
