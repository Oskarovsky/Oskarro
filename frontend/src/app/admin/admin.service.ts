import {Injectable} from '@angular/core';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Observable} from 'rxjs';
import {environment} from '../../environments/environment';

export interface Page<T> { items: T[]; page: number; size: number; total: number; }

export interface UserRow {
  id: number; username: string; email: string; createdAt: string; lastLoginAt: string; lastSeenAt: string;
  activated: boolean; banned: boolean; banReason: string; roles: string[]; rankOverride: string;
  tracks: number; comments: number; votes: number;
}

export interface TrackRow {
  id: number; title: string; artist: string; version: string; genre: string; url: string;
  author: string; createdAt: string; score: number; views: number; comments: number;
}

export interface CommentRow { id: number; text: string; author: string; trackId: number; trackTitle: string; createdAt: string; }
export interface VoteRow { trackId: number; trackTitle: string; vote: number; createdAt: string; }
export interface AdminEvent { type: string; at: string; actor: string; text: string; targetType: string; targetId: number; }
export interface AuditRow { id: number; actor: string; action: string; targetType: string; targetId: number; targetLabel: string; details: string; createdAt: string; }

export interface Overview {
  users: number; newUsers7d: number; newUsers30d: number; active24h: number; active7d: number; active30d: number;
  notActivated: number; banned: number; staff: number; openReports: number; tracks: number; tracks7d: number; comments: number;
  comments7d: number; votes: number; plays: number; plays7d: number; latestUsers: UserRow[]; latestEvents: AdminEvent[];
}

export interface UserDetail {
  user: UserRow; bio: string; website: string; tracks: TrackRow[]; comments: CommentRow[]; votes: VoteRow[]; audit: AuditRow[];
}

export interface ReportRow {
  id: number; reason: string; trackId: number; trackTitle: string; url: string; details: string;
  reporterName: string; reporterEmail: string; reporterUser: string; status: string;
  resolution: string; resolvedBy: string; resolvedAt: string; createdAt: string;
}

export interface UserChange { activated?: boolean; banned?: boolean; banReason?: string; roles?: string[]; rankOverride?: string; }

const API = environment.serverUrl + '/admin';

@Injectable({providedIn: 'root'})
export class AdminService {
  constructor(private http: HttpClient) {}

  overview(): Observable<Overview> {
    return this.http.get<Overview>(API + '/overview');
  }

  users(q: string, filter: string, sort: string, page: number, size = 25): Observable<Page<UserRow>> {
    return this.http.get<Page<UserRow>>(API + '/users', {params: new HttpParams({fromObject: {q, filter, sort, page, size}})});
  }

  user(id: number): Observable<UserDetail> {
    return this.http.get<UserDetail>(API + '/users/' + id);
  }

  changeUser(id: number, change: UserChange): Observable<UserDetail> {
    return this.http.patch<UserDetail>(API + '/users/' + id, change);
  }

  tracks(q: string, genre: string, page: number, size = 25): Observable<Page<TrackRow>> {
    return this.http.get<Page<TrackRow>>(API + '/tracks', {params: new HttpParams({fromObject: {q, genre, page, size}})});
  }

  changeTrack(id: number, change: Partial<TrackRow>): Observable<TrackRow> {
    return this.http.put<TrackRow>(API + '/tracks/' + id, change);
  }

  deleteTrack(id: number): Observable<void> {
    return this.http.delete<void>(API + '/tracks/' + id);
  }

  comments(q: string, page: number, size = 25): Observable<Page<CommentRow>> {
    return this.http.get<Page<CommentRow>>(API + '/comments', {params: new HttpParams({fromObject: {q, page, size}})});
  }

  changeComment(id: number, text: string): Observable<CommentRow> {
    return this.http.put<CommentRow>(API + '/comments/' + id, {text});
  }

  deleteComment(id: number): Observable<void> {
    return this.http.delete<void>(API + '/comments/' + id);
  }

  activity(type: string, page: number, size = 30): Observable<Page<AdminEvent>> {
    return this.http.get<Page<AdminEvent>>(API + '/activity', {params: new HttpParams({fromObject: {type, page, size}})});
  }

  reports(status: string): Observable<ReportRow[]> {
    return this.http.get<ReportRow[]>(API + '/reports', {params: {status}});
  }

  resolveReport(id: number, status: string, resolution: string): Observable<ReportRow> {
    return this.http.patch<ReportRow>(API + '/reports/' + id, {status, resolution});
  }

  audit(page: number, size = 30): Observable<Page<AuditRow>> {
    return this.http.get<Page<AuditRow>>(API + '/audit', {params: new HttpParams({fromObject: {page, size}})});
  }
}
