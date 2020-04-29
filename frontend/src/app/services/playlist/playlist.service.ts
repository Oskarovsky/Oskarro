import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Playlist } from '../../playlists/playlist/model/playlist';
import { Track } from 'src/app/tracks/track/model/track';
import {environment} from '../../../environments/environment';

const API: string = environment.serverUrl;
const PLAYLIST_API = API + '/playlist';

@Injectable({providedIn: 'root'})
export class PlaylistService {

  tracks: Track[] = [];

  httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private http: HttpClient) { }

  /** GET all playlists */
  getAllPlaylists(): Observable<Playlist[]> {
    return this.http.get<Playlist[]>(PLAYLIST_API + '/findAll');
  }

  /** GET all playlists from User */
  getAllPlaylistsByUsername(username: string): Observable<Playlist[]> {
    return this.http.get<Playlist[]>(PLAYLIST_API + '/all/' + username);
  }

  /** GET playlist by id */
  getPlaylist(id: string): Observable<any> {
    return this.http.get<Playlist>(PLAYLIST_API + '/' + id);
  }

  /** GET all tracks from playlist */
  getAllTracksFromPlaylist(playlistId: string): Observable<Track[]> {
    return this.http.get<Track[]>(PLAYLIST_API + '/' + playlistId + '/tracks');
  }

  addPlaylist(playlist: Playlist): Observable<Playlist> {
    return this.http.post<Playlist>(PLAYLIST_API  + '/add', playlist);
  }

  deletePlaylist(id: number): Observable<any> {
    return this.http.delete<Playlist>(PLAYLIST_API + '/' + id);
  }
}
