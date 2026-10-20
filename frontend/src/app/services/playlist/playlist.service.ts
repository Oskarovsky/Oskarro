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

  /** Playlists the signed-in user may play in the radio. */
  playable(): Observable<Playlist[]> {
    return this.http.get<Playlist[]>(PLAYLIST_API + '/playable');
  }

  access(): Observable<{canBrowseAll: boolean, rank: string, signedIn: boolean}> {
    return this.http.get<{canBrowseAll: boolean, rank: string, signedIn: boolean}>(PLAYLIST_API + '/access');
  }

  updatePlaylist(id: number, draft: {name: string, description: string, privatePlaylist?: boolean}): Observable<Playlist> {
    return this.http.put<Playlist>(PLAYLIST_API + '/' + id, draft);
  }

  addPlaylist(draft: {name: string, description: string, privatePlaylist?: boolean}): Observable<Playlist> {
    return this.http.post<Playlist>(PLAYLIST_API + '/add', draft);
  }

  deletePlaylist(id: number): Observable<any> {
    return this.http.delete<Playlist>(PLAYLIST_API + '/' + id);
  }

  pin(playlistId: number, trackId: number): Observable<{name: string}> {
    return this.http.post<{name: string}>(PLAYLIST_API + '/' + playlistId + '/pin/' + trackId, {});
  }

  unpin(playlistId: number, trackId: number): Observable<void> {
    return this.http.delete<void>(PLAYLIST_API + '/' + playlistId + '/pin/' + trackId);
  }
}
