import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { TokenStorageService } from '../services/auth/token-storage.service';
import { TrackSearch } from '../services/track-search';

@Component({
  selector: 'app-navigation',
  templateUrl: './navigation.component.html',
  styleUrls: ['./navigation.component.css'],
  standalone: false
})
export class NavigationComponent implements OnInit {

  private roles: string[];
  isLoggedIn = false;
  showAdminBoard = false;
  showModeratorBoard = false;
  username: string;
  menuOpen = false;

  constructor(private http: HttpClient,
              private tokenStorageService: TokenStorageService,
              private router: Router,
              public search: TrackSearch) {}

  title = 'Oskarro.com';

  ngOnInit() {
    this.isLoggedIn = !!this.tokenStorageService.getToken();

    if (this.isLoggedIn) {
      const user = this.tokenStorageService.getUser();
      this.roles = user.roles;

      this.showAdminBoard = this.roles.includes('ROLE_ADMIN');
      this.showModeratorBoard = this.roles.includes('ROLE_MODERATOR');

      this.username = user.username;
    }
  }

  logout() {
    this.tokenStorageService.signOut();
    window.location.reload();
  }

  onSearch(event: Event) {
    const form = event.target as HTMLFormElement;
    const input = form?.querySelector('input');
    const q = (input?.value ?? this.search.text).trim();
    this.search.text = q;
    this.menuOpen = false;
    this.router.navigate(['/szukaj'], {queryParams: q ? {q} : {}});
  }

}
