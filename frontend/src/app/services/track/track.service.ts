import { Injectable } from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import { Observable } from 'rxjs';
import { Track } from 'src/app/tracks/track/model/track';
import {environment} from '../../../environments/environment';

const API: string = environment.serverUrl;
const TRACK_API = API + '/tracks';
const PROVIDER_API = API + '/providers';

@Injectable({providedIn: 'root'})
export class TrackService {

  constructor(private http: HttpClient) {
  }

  getAllTracks(): Observable<Track[]> {
    return this.http.get<Track[]>(TRACK_API + '/findAll');
  }

  getTrackById(id: string) {
    return this.http.get(TRACK_API + '/' + id);
  }

  getTracksFromProviderByGenre(id: string, genre: string) {
    return this.http.get(PROVIDER_API + '/' + id + '/' + genre);
  }

  getTracksByProviderId(id: string) {
    return this.http.get(PROVIDER_API + '/' + id + '/tracks');
  }

  getTracksByGenre(genre: string) {
    return this.http.get(TRACK_API + '/genre/' + genre);
  }

  getTracksByProviderName(providerName: string) {
    return this.http.get(PROVIDER_API + '/' + providerName + '/all-tracks')
  }

  addTrackToRanking(track: Track): Observable<any> {
    return this.http.post<Track>(TRACK_API + '/addToRanking', track);
  }

  saveTrackToPlaylist(track: Track): Observable<Track> {
    return this.http.post<Track>(TRACK_API + '/add', track);
  }

  saveTrackToVideo(track: Track): Observable<Track> {
    return this.http.post<Track>(TRACK_API + '/add', track);
  }

  deleteTrackFromPlaylist(id: number): Observable<any> {
    return this.http.delete(TRACK_API + '/' + id);
  }

}
