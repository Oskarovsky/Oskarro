import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
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
import { ProviderComponent } from './providers/provider/provider.component';
import { MatGridListModule } from '@angular/material/grid-list';
import { ProviderListComponent } from './providers/provider-list/provider-list.component';
import { ProviderDetailsComponent } from './providers/provider-details/provider-details.component';
import { AddProviderComponent } from './providers/provider-add/add-provider.component';
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
import { authInterceptorProviders } from './services/auth/auth.interceptor';
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
import { VideoAddComponent } from './videos/video-add/video-add.component';
import {VideoFilterPipe} from './services/video/video-filter.pipe';
import { VideoDetailsComponent } from './videos/video-details/video-details.component';
import { VideoEditComponent } from './videos/video-edit/video-edit.component';
import { PlaylistAllComponent } from './playlists/playlist-all/playlist-all.component';
import { HomeComponent } from './home/home.component';
import { FooterComponent } from './footer/footer.component';


@NgModule({
  declarations: [
    AppComponent,
    TrackComponent,
    TrackListComponent,
    ProviderComponent,
    ProviderListComponent,
    ProviderDetailsComponent,
    AddProviderComponent,
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
    VideoAddComponent,
    VideoFilterPipe,
    VideoDetailsComponent,
    VideoEditComponent,
    PlaylistAllComponent,
    HomeComponent,
    FooterComponent
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
    provideAnimations(),
    provideHttpClient(withInterceptorsFromDi()),
    ...authInterceptorProviders
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
