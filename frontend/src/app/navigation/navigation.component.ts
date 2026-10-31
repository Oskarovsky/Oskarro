import { Component, ElementRef, HostListener, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { TokenStorageService } from '../services/auth/token-storage.service';
import { TrackSearch } from '../services/track-search';
import { GENRES } from '../tracks/genres';
import { LangService } from '../lang.service';

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
  searchOpen = false;
  genres = GENRES;

  constructor(private http: HttpClient,
              private tokenStorageService: TokenStorageService,
              private router: Router,
              private host: ElementRef<HTMLElement>,
              public search: TrackSearch,
              readonly lang: LangService) {}

  title = 'VIXA';

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

  get onMusic() {
    return this.router.url.startsWith('/tracklist/');
  }

  get onSearchPage() {
    return this.router.url.startsWith('/szukaj');
  }

  toggleSearch() {
    this.searchOpen = !this.searchOpen;
    this.menuOpen = false;
    if (this.searchOpen) {
      setTimeout(() => {
        const field = this.host.nativeElement.querySelector('.search-drawer input');
        if (field instanceof HTMLInputElement) {
          field.focus();
        }
      });
    }
  }

  closeMusic(menu: HTMLDetailsElement) {
    menu.open = false;
    this.menuOpen = false;
  }

  closeUser(menu: HTMLDetailsElement) {
    menu.open = false;
    this.menuOpen = false;
  }

  @HostListener('document:pointermove', ['$event'])
  closeMenusWhenFar(event: PointerEvent) {
    if (event.pointerType && event.pointerType !== 'mouse') {
      return;
    }
    const menus = this.host.nativeElement.querySelectorAll('details.nav-drop[open]');
    menus.forEach(menu => {
      if (menu instanceof HTMLDetailsElement && this.pointerFarFrom(menu, event.clientX, event.clientY)) {
        menu.open = false;
      }
    });
  }

  private pointerFarFrom(menu: HTMLElement, x: number, y: number) {
    const pop = menu.querySelector('.menu-pop');
    const boxes = [menu.getBoundingClientRect()];
    if (pop instanceof HTMLElement) {
      boxes.push(pop.getBoundingClientRect());
    }
    const reach = 72;
    const left = Math.min(...boxes.map(box => box.left)) - reach;
    const right = Math.max(...boxes.map(box => box.right)) + reach;
    const top = Math.min(...boxes.map(box => box.top)) - reach;
    const bottom = Math.max(...boxes.map(box => box.bottom)) + reach;
    return x < left || x > right || y < top || y > bottom;
  }

  onSearch(event: Event) {
    const form = event.target as HTMLFormElement;
    const input = form?.querySelector('input');
    const q = (input?.value ?? this.search.text).trim();
    this.search.text = q;
    this.menuOpen = false;
    this.searchOpen = false;
    this.router.navigate(['/szukaj'], {queryParams: q ? {q} : {}});
  }

  @HostListener('document:keydown.escape')
  closeSearch() {
    this.searchOpen = false;
  }

}
