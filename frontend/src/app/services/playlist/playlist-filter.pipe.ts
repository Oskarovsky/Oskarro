import { Pipe, PipeTransform } from '@angular/core';
import {Playlist} from '../../playlists/playlist/model/playlist';

@Pipe({
  name: 'playlistFilter',
  standalone: false
})
export class PlaylistFilterPipe implements PipeTransform {

  transform(playlists: Playlist[], text: string): Playlist[] {
    if (text == null || text === '') {
      return playlists;
    }
    return playlists.filter(n => n.name.includes(text));
  }

}
