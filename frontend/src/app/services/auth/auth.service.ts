import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import {TokenStorageService} from './token-storage.service';
import { environment } from 'src/environments/environment';

const API: string = environment.serverUrl;
const AUTH_API = API + '/auth';

const httpOptions = {
  headers: new HttpHeaders({ 'Content-Type': 'application/json' })
};

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor(private httpClient: HttpClient,
              private tokenStorageService: TokenStorageService) { }

  login(credentials): Observable<any> {
    return this.httpClient.post(AUTH_API + '/signin', {
      username: credentials.username,
      password: credentials.password
    }, httpOptions);
  }

  providers(): Observable<string[]> {
    return this.httpClient.get<string[]>(AUTH_API + '/providers');
  }

  session(token: string): Observable<any> {
    return this.httpClient.get(AUTH_API + '/session', {headers: {Authorization: 'Bearer ' + token}});
  }

  register(user): Observable<any> {
    return this.httpClient.post(AUTH_API + '/signup', {
      username: user.username,
      email: user.email,
      password: user.password,
      acceptTerms: !!user.terms,
      hp: user.hp || ''
    }, httpOptions);
  }

}
