import {NgModule} from '@angular/core';
import {LegalComponent} from './legal/legal.component';
import {ReportComponent} from './legal/report.component';
import {SsoDoneComponent} from './auth/sso-done.component';
import {SetListComponent} from './tracks/set-list/set-list.component';
import {AdminLayoutComponent} from './admin/admin-layout.component';
import {AdminDashboardComponent} from './admin/admin-dashboard.component';
import {AdminUsersComponent} from './admin/admin-users.component';
import {AdminUserComponent} from './admin/admin-user.component';
import {AdminTracksComponent} from './admin/admin-tracks.component';
import {AdminCommentsComponent} from './admin/admin-comments.component';
import {AdminReportsComponent} from './admin/admin-reports.component';
import {AdminActivityComponent} from './admin/admin-activity.component';
import {adminGuard} from './admin/admin.guard';
import {RouterModule, Routes} from '@angular/router';
import {TrackComponent} from './tracks/track/track.component';
import {TrackListComponent} from './tracks/track-list/track-list.component';
import {NotFoundComponent} from './not-found/not-found.component';
import {ToplistComponent} from './playlists/toplist/toplist.component';
import {ToplistEditComponent} from './playlists/toplist-edit/toplist-edit.component';
import {PlaylistComponent} from './playlists/playlist/playlist.component';
import {PlaylistAddComponent} from './playlists/playlist-add/playlist-add.component';
import {PlaylistEditComponent} from './playlists/playlist-edit/playlist-edit.component';
import {PlaylistDetailsComponent} from './playlists/playlist-details/playlist-details.component';
import {LoginComponent} from './auth/login/login.component';
import {RegisterComponent} from './auth/register/register.component';
import {BoardModeratorComponent} from './user/board-moderator/board-moderator.component';
import {BoardUserComponent} from './user/board-user/board-user.component';
import {VideoComponent} from './videos/video/video.component';
import {ChannelSetComponent} from './videos/channel-set/channel-set.component';
import {VideoAddComponent} from './videos/video-add/video-add.component';
import {VideoDetailsComponent} from './videos/video-details/video-details.component';
import {VideoEditComponent} from './videos/video-edit/video-edit.component';
import {PlaylistAllComponent} from './playlists/playlist-all/playlist-all.component';
import {ProfileComponent} from './profile/profile.component';
import {HomeComponent} from './home/home.component';
import {TrackFormComponent} from './tracks/track-form/track-form.component';
import {RankingComponent} from './ranking/ranking.component';
import {SearchComponent} from './search/search.component';
import {RadioComponent} from './tracks/radio/radio.component';
import {PasswordResetComponent} from './auth/password-reset.component';
import {ActivateAccountComponent} from './auth/activate.component';


const routes: Routes = [
  { path: 'tracklist', component: TrackListComponent },
  { path: '', component: HomeComponent },
  { path: 'track/add', component: TrackFormComponent },
  { path: 'track/:id/edit', component: TrackFormComponent },
  { path: 'track/:id', component: TrackComponent },
  { path: 'tracklist/:genre', component: TrackListComponent },
  { path: 'toplist/:genre', component: ToplistComponent },
  { path: 'sety', component: SetListComponent },
  { path: 'sety/:kategoria', component: SetListComponent },
  { path: 'playlist/all', component: PlaylistAllComponent },
  { path: 'playlist/all/:username', component: PlaylistComponent },
  { path: 'toplist/:genre/edit', component: ToplistEditComponent },
  { path: 'playlist/add', component: PlaylistAddComponent },
  { path: 'playlist/:id/edit', component: PlaylistEditComponent },
  { path: 'playlist/:id/details', component: PlaylistDetailsComponent },
  { path: 'login', component: LoginComponent },
  { path: 'logowanie/sso', component: SsoDoneComponent },
  { path: 'regulamin', component: LegalComponent, data: {doc: 'terms'} },
  { path: 'polityka-prywatnosci', component: LegalComponent, data: {doc: 'privacy'} },
  { path: 'cookies', component: LegalComponent, data: {doc: 'cookies'} },
  { path: 'zglos-naruszenie', component: ReportComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'przypomnij', component: PasswordResetComponent, data: {mode: 'forgot'} },
  { path: 'haslo', component: PasswordResetComponent, data: {mode: 'reset'} },
  { path: 'aktywuj', component: ActivateAccountComponent },
  { path: 'ranking', component: RankingComponent },
  { path: 'radio', component: RadioComponent },
  { path: 'szukaj', component: SearchComponent },
  { path: 'profile', component: ProfileComponent },
  { path: 'profile/:username', component: ProfileComponent },
  { path: 'user', component: BoardUserComponent },
  { path: 'mod', component: BoardModeratorComponent },
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [adminGuard],
    children: [
      { path: '', component: AdminDashboardComponent },
      { path: 'uzytkownicy', component: AdminUsersComponent },
      { path: 'uzytkownicy/:id', component: AdminUserComponent },
      { path: 'utwory', component: AdminTracksComponent },
      { path: 'komentarze', component: AdminCommentsComponent },
      { path: 'zgloszenia', component: AdminReportsComponent },
      { path: 'aktywnosc', component: AdminActivityComponent },
      { path: 'dziennik', component: AdminActivityComponent, data: {audit: true} }
    ]
  },
  { path: 'video', component: VideoComponent },
  { path: 'video/set/:id', component: ChannelSetComponent },
  { path: 'video/category/:category', redirectTo: '/video', pathMatch: 'full' },
  { path: 'video/:id/edit', component: VideoEditComponent },
  { path: 'video/:id/details', component: VideoDetailsComponent },
  { path: 'video/add', component: VideoAddComponent },
  { path: '**', component: NotFoundComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
