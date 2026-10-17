import {Playlist} from '../../../playlists/playlist/model/playlist';
import {Video} from '../../../videos/video/model/video';

export interface Track {
  id: number;
  title: string;
  artist: string;
  points: number;
  genre: string;
  version: string;
  url: string;
  position: number;
  playlist: Playlist;
  video: Video;
  createdAt?: string;
  score?: number;
  hasCover?: boolean;
  /** 'set' for DJ sets and mixes. */
  kind?: string;
  setCategory?: string;
  eventName?: string;
  tracklist?: string;
}
