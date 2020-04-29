import { Component, OnInit } from '@angular/core';
import {PlaylistService} from '../../services/playlist/playlist.service';
import {Playlist} from '../playlist/model/playlist';
import {Track} from '../../tracks/track/model/track';
import {TrackService} from '../../services/track/track.service';
import {TokenStorageService} from '../../services/auth/token-storage.service';
import { User } from '../../services/user/user';

@Component({
  selector: 'app-playlist-add',
  templateUrl: './playlist-add.component.html',
  styleUrls: ['./playlist-add.component.css']
})
export class PlaylistAddComponent implements OnInit {

  playlists: Playlist[] = [];
  tracks: Track[] = [];
  isUserLogged = false;
  user: null;

  modelPlaylist: Playlist = {
    id: null,
    name: '',
    user: null
  };

  modelUser: User = {
    id: null,
    username: '',
    email: '',
    password: ''
  };

  constructor(private playlistService: PlaylistService,
              private trackService: TrackService,
              private tokenStorageService: TokenStorageService) { }

  ngOnInit() {
    this.isUserLogged = !!this.tokenStorageService.getToken();
    if (this.isUserLogged) {
      this.modelUser.username = this.tokenStorageService.getUser().username;
      this.modelUser.id = this.tokenStorageService.getUser().id;
      this.modelUser.email = this.tokenStorageService.getUser().email;
      this.modelUser.password = this.tokenStorageService.getUser().password;
    }
    this.getAllPlaylists();
  }

  public getAllPlaylists() {
    this.playlistService.getAllPlaylists().subscribe(
      res => {
        this.playlists = res;
      },
      error => {
        alert('An error with fetching playlists has occurred');
      }
    );
  }
  createPlaylist(name: string) {
    const newPlaylist: Playlist = {
      id: null,
      name,
      user: this.modelUser
    };
    this.playlistService.addPlaylist(newPlaylist).subscribe(
      result => {
        newPlaylist.name = name;
        this.playlists.push(newPlaylist);
      },
      error => {
        alert('An error has occurred while saving the playlist');
      }
    );
  }

}
