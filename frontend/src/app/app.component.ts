import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { NavigationEnd, Router } from '@angular/router';
import { TokenStorageService } from './services/auth/token-storage.service';
import { SeoService } from './seo.service';
import { StatsService } from './services/stats.service';
import { LangService } from './lang.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  standalone: false
})

export class AppComponent implements OnInit {

  private roles: string[];
  isLoggedIn = false;
  showAdminBoard = false;
  showModeratorBoard = false;
  username: string;

  constructor(private http: HttpClient,
              private tokenStorageService: TokenStorageService,
              private router: Router,
              private seo: SeoService,
              private stats: StatsService,
              private lang: LangService) {}

  ngOnInit() {
    this.stats.load().subscribe({error: () => {}});
    this.seo.applyRoute(this.router.url);
    this.lang.changes.subscribe(() => this.seo.applyRoute(this.router.url));
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        this.seo.applyRoute(event.urlAfterRedirects);
      }
    });
  }


}
