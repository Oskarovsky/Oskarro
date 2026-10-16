import { Injectable } from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import { Observable } from 'rxjs';
import { Track } from 'src/app/tracks/track/model/track';
import {environment} from '../../../environments/environment';

const API: string = environment.serverUrl;
const TRACK_API = API + '/tracks';

@Injectable({providedIn: 'root'})
export class TrackService {

  constructor(private http: HttpClient) {
  }

  getAllTracks(): Observable<Track[]> {
    return this.http.get<Track[]>(TRACK_API + '/findAll');
  }

  getTrackById(id: string) {
    return this.http.get(TRACK_API + '/id/' + id);
  }

  fileFacts(id: number) {
    return this.http.get<{bytes: number, bitrate: number, sampleRate: number, channels: number, bitDepth: number, bpm: number, format: string}>(
      TRACK_API + '/' + id + '/file');
  }

  getTracksByGenre(genre: string, sort = 'new') {
    return this.http.get(TRACK_API + '/genre/' + genre, {params: {sort}});
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

  shareTrack(track: Track): Observable<Track> {
    return this.http.post<Track>(TRACK_API + '/share', track);
  }

  updateTrack(id: number, track: Track): Observable<Track> {
    return this.http.put<Track>(TRACK_API + '/' + id, track);
  }

  getComments(id: string): Observable<Array<{id: number, text: string, author: string, createdAt: string}>> {
    return this.http.get<Array<{id: number, text: string, author: string, createdAt: string}>>(TRACK_API + '/' + id + '/comments');
  }

  addComment(id: string, text: string) {
    return this.http.post<{id: number, text: string, author: string, createdAt: string}>(
      TRACK_API + '/' + id + '/comments', {text});
  }

  getLikes(id: string) {
    return this.http.get<{likes: number, dislikes: number, score: number, mine: number}>(TRACK_API + '/' + id + '/likes');
  }

  vote(id: string, vote: number) {
    return this.http.post<{likes: number, dislikes: number, score: number, mine: number}>(
      TRACK_API + '/' + id + '/vote', {vote});
  }

  coverUrl(id: number) {
    return TRACK_API + '/' + id + '/cover';
  }

  uploadCover(id: number, file: File) {
    const data = new FormData();
    data.append('file', file);
    return this.http.post<{hasCover: boolean}>(TRACK_API + '/' + id + '/cover', data);
  }

}
