export class Playlist {
  id: number;
  name: string;
  description: string;
  owner: string;
  tracks: number;
  /** Visible and playable only by the owner. */
  privatePlaylist?: boolean;
  /** The signed-in user owns it. */
  mine?: boolean;
}
