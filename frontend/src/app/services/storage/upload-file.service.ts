import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpRequest } from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../../environments/environment';

const API: string = environment.serverUrl;
const STORAGE_API = API + '/storage';

@Injectable({
  providedIn: 'root'
})
export class UploadFileService {

  constructor(private http: HttpClient) {
  }

  upload(file: File, username: string): Observable<HttpEvent<any>> {
    const formData: FormData = new FormData();
    formData.append('file', file);
    formData.append('username', username);
    const req = new HttpRequest('POST', STORAGE_API + '/upload', formData, {
      reportProgress: true,
      responseType: 'json'
    });
    return this.http.request(req);
  }

  getFile(username: string): Observable<Blob> {
    return this.http.get(STORAGE_API + '/' + username + '/avatar', { responseType: 'blob' });
  }
}
