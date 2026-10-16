import {Component, OnInit} from '@angular/core';
import {RefreshAfterHttpDirective} from '../../services/ui-refresh';
import {Playlist} from './model/playlist';
import {PlaylistService} from '../../services/playlist/playlist.service';
import {ActivatedRoute} from '@angular/router';
import {TokenStorageService} from '../../services/auth/token-storage.service';

@Component({
  selector: 'app-playlist',
  templateUrl: './playlist.component.html',
  styleUrls: ['./playlist.component.css'],
  standalone: false,
  hostDirectives: [RefreshAfterHttpDirective]
})
export class PlaylistComponent implements OnInit {

  playlists: Playlist[] = [];
  owner = '';
  mine = false;
  loaded = false;

  constructor(private playlistService: PlaylistService,
              private tokenStorage: TokenStorageService,
              private route: ActivatedRoute) { }

  ngOnInit() {
    const user = this.tokenStorage.getUser();
    this.route.params.subscribe(params => {
      this.owner = params.username || '';
      this.mine = !!this.tokenStorage.getToken() && user?.username === this.owner;
      this.loaded = false;
      this.playlistService.getAllPlaylistsByUsername(this.owner).subscribe(response => {
        this.playlists = response || [];
        this.loaded = true;
      });
    });
  }

  deletePlaylist(id: number) {
    if (!confirm('Usunąć tę składankę razem z jej utworami?')) {
      return;
    }
    this.playlistService.deletePlaylist(id).subscribe(() => {
      this.playlists = this.playlists.filter(playlist => playlist.id !== id);
    });
  }
}
