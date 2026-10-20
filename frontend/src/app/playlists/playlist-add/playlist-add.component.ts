import {Component, OnInit} from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import {PlaylistService} from '../../services/playlist/playlist.service';
import {TokenStorageService} from '../../services/auth/token-storage.service';
import {Router} from '@angular/router';

@Component({
  selector: 'app-playlist-add',
  templateUrl: './playlist-add.component.html',
  styleUrls: ['./playlist-add.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class PlaylistAddComponent implements OnInit {

  loggedIn = false;
  busy = false;
  error = '';
  name = '';
  privatePlaylist = false;
  description = '';

  constructor(private playlistService: PlaylistService,
              private tokenStorage: TokenStorageService,
              private router: Router) { }

  ngOnInit() {
    this.loggedIn = !!this.tokenStorage.getToken();
  }

  create() {
    const name = this.name.trim();
    const description = this.description.trim();
    if (name.length < 2) {
      this.error = 'Nazwa musi mieć przynajmniej 2 znaki.';
      return;
    }
    this.busy = true;
    this.error = '';
    this.playlistService.addPlaylist({name, description, privatePlaylist: this.privatePlaylist}).subscribe({
      next: saved => this.router.navigate(['/playlist', saved.id, 'edit']),
      error: () => {
        this.busy = false;
        this.error = 'Nie udało się utworzyć składanki. Zaloguj się i spróbuj jeszcze raz.';
      }
    });
  }
}
