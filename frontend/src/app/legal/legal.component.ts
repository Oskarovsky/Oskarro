import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

/** Regulamin, polityka prywatności i cookies — one component, the document comes from route data. */
@Component({
  selector: 'app-legal',
  templateUrl: './legal.component.html',
  standalone: false
})
export class LegalComponent implements OnInit {
  doc: 'terms' | 'privacy' | 'cookies' = 'terms';
  readonly updated = '17 października 2026';
  readonly contact = 'oskar.slyk@gmail.com';

  constructor(private route: ActivatedRoute) {}

  ngOnInit() {
    this.route.data.subscribe(data => {
      this.doc = data['doc'] || 'terms';
      window.scrollTo({top: 0});
    });
  }
}
