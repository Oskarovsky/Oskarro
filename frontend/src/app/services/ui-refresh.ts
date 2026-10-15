import { ChangeDetectorRef, Directive, Injectable, OnDestroy, OnInit } from '@angular/core';

@Injectable({providedIn: 'root'})
export class UiRefresh {
  private readonly views = new Set<ChangeDetectorRef>();

  register(view: ChangeDetectorRef) {
    this.views.add(view);
  }

  unregister(view: ChangeDetectorRef) {
    this.views.delete(view);
  }

  refresh() {
    for (const view of this.views) {
      try {
        view.detectChanges();
      } catch {
        this.views.delete(view);
      }
    }
  }
}

@Directive({
  selector: '[appRefreshAfterHttp]',
  standalone: true
})
export class RefreshAfterHttpDirective implements OnInit, OnDestroy {
  constructor(private view: ChangeDetectorRef, private uiRefresh: UiRefresh) {}

  ngOnInit() {
    this.uiRefresh.register(this.view);
  }

  ngOnDestroy() {
    this.uiRefresh.unregister(this.view);
  }
}
