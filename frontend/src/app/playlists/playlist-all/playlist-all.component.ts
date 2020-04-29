import { Component, OnInit } from '@angular/core';
import {Playlist} from '../playlist/model/playlist';
import {PlaylistService} from '../../services/playlist/playlist.service';

@Component({
  selector: 'app-playlist-all',
  templateUrl: './playlist-all.component.html',
  styleUrls: ['./playlist-all.component.css']
})
export class PlaylistAllComponent implements OnInit {

  playlists: Playlist[] = [];
  searchText: string;

  constructor(private playlistService: PlaylistService) { }

  ngOnInit() {
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
}
