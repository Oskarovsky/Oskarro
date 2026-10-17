import { BrowserModule } from '@angular/platform-browser';
import { NgModule, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors, withInterceptorsFromDi } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatToolbarModule } from '@angular/material/toolbar';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { TrackComponent } from './tracks/track/track.component';
import { TrackListComponent } from './tracks/track-list/track-list.component';
import { MatGridListModule } from '@angular/material/grid-list';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { NavigationComponent } from './navigation/navigation.component';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { NotFoundComponent } from './not-found/not-found.component';
import { ToplistComponent } from './playlists/toplist/toplist.component';
import { MatSelectModule } from '@angular/material/select';
import { ToplistEditComponent } from './playlists/toplist-edit/toplist-edit.component';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSortModule } from '@angular/material/sort';
import { authInterceptorProviders, zoneHttpInterceptor } from './services/auth/auth.interceptor';
import { PlaylistComponent } from './playlists/playlist/playlist.component';
import { PlaylistAddComponent } from './playlists/playlist-add/playlist-add.component';
import { PlaylistEditComponent } from './playlists/playlist-edit/playlist-edit.component';
import { PlaylistDetailsComponent } from './playlists/playlist-details/playlist-details.component';
import { PlaylistFilterPipe } from './services/playlist/playlist-filter.pipe';
import { LoginComponent } from './auth/login/login.component';
import { RegisterComponent } from './auth/register/register.component';
import { ProfileComponent } from './profile/profile.component';
import { BoardAdminComponent } from './user/board-admin/board-admin.component';
import { BoardModeratorComponent } from './user/board-moderator/board-moderator.component';
import { BoardUserComponent } from './user/board-user/board-user.component';
import { VideoComponent } from './videos/video/video.component';
import { ChannelSetComponent } from './videos/channel-set/channel-set.component';
import { VideoAddComponent } from './videos/video-add/video-add.component';
import {VideoFilterPipe} from './services/video/video-filter.pipe';
import { VideoDetailsComponent } from './videos/video-details/video-details.component';
import { VideoEditComponent } from './videos/video-edit/video-edit.component';
import { PlaylistAllComponent } from './playlists/playlist-all/playlist-all.component';
import { HomeComponent } from './home/home.component';
import { ActivityChartComponent } from './home/activity-chart.component';
import { GrowthBoardComponent } from './home/growth-board.component';
import { GenreArtComponent } from './home/genre-art.component';
import { AdminLayoutComponent } from './admin/admin-layout.component';
import { AdminDashboardComponent } from './admin/admin-dashboard.component';
import { AdminUsersComponent } from './admin/admin-users.component';
import { AdminUserComponent } from './admin/admin-user.component';
import { AdminTracksComponent } from './admin/admin-tracks.component';
import { AdminCommentsComponent } from './admin/admin-comments.component';
import { AdminActivityComponent } from './admin/admin-activity.component';
import { AdminFeedComponent } from './admin/admin-feed.component';
import { AgoPipe } from './admin/ago.pipe';

import { FooterComponent } from './footer/footer.component';
import { TrackPlayerComponent } from './tracks/kraken-player/kraken-player.component';
import { TrackFormComponent } from './tracks/track-form/track-form.component';
import { RankingComponent } from './ranking/ranking.component';
import { SearchComponent } from './search/search.component';
import { VersionLabelPipe } from './tracks/version-label.pipe';
import { RankMarkPipe } from './tracks/rank-mark.pipe';
import { PersonComponent } from './tracks/person.component';
import { CoverArtComponent } from './tracks/cover-art.component';
import { ScoreBadgeComponent } from './tracks/score-badge/score-badge.component';
import { AdSlotComponent } from './ads/ad-slot.component';
import { DeckComponent } from './tracks/deck/deck.component';
import { NowBarComponent } from './tracks/now-bar.component';
import { RadioComponent } from './tracks/radio/radio.component';
import { PagerComponent } from './tracks/pager.component';
import { TrackFactsComponent } from './tracks/track-facts.component';
import { TPipe } from './t.pipe';
import { PasswordResetComponent } from './auth/password-reset.component';
import { ActivateAccountComponent } from './auth/activate.component';


@NgModule({
  declarations: [
    AppComponent,
    TrackComponent,
    TrackFormComponent,
    TrackListComponent,
    NavigationComponent,
    NotFoundComponent,
    ToplistComponent,
    ToplistEditComponent,
    PlaylistComponent,
    PlaylistAddComponent,
    PlaylistEditComponent,
    PlaylistDetailsComponent,
    PlaylistFilterPipe,
    LoginComponent,
    RegisterComponent,
    ProfileComponent,
    BoardAdminComponent,
    BoardModeratorComponent,
    BoardUserComponent,
    VideoComponent,
    ChannelSetComponent,
    VideoAddComponent,
    VideoFilterPipe,
    VideoDetailsComponent,
    VideoEditComponent,
    PlaylistAllComponent,
    HomeComponent,
    ActivityChartComponent,
    GrowthBoardComponent,
    GenreArtComponent,
    AdminLayoutComponent,
    AdminDashboardComponent,
    AdminUsersComponent,
    AdminUserComponent,
    AdminTracksComponent,
    AdminCommentsComponent,
    AdminActivityComponent,
    AdminFeedComponent,
    AgoPipe,
    FooterComponent,
    TrackPlayerComponent,
    RankingComponent,
    SearchComponent,
    VersionLabelPipe,
    RankMarkPipe,
    PersonComponent,
    CoverArtComponent,
    ScoreBadgeComponent,
    AdSlotComponent,
    DeckComponent,
    NowBarComponent,
    RadioComponent,
    PagerComponent,
    TrackFactsComponent,
    TPipe,
    PasswordResetComponent,
    ActivateAccountComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    MatButtonModule,
    MatCardModule,
    MatInputModule,
    MatListModule,
    MatToolbarModule,
    MatGridListModule,
    FormsModule,
    MatMenuModule,
    MatIconModule,
    MatSelectModule,
    ReactiveFormsModule,
    MatTableModule,
    MatTabsModule,
    MatSortModule,
    MatExpansionModule,
  ],
  providers: [
    provideZoneChangeDetection(),
    provideAnimations(),
    provideHttpClient(withInterceptors([zoneHttpInterceptor]), withInterceptorsFromDi()),
    ...authInterceptorProviders
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
