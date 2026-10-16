export {hostujeId} from './audio-hosts';
import {hostujeId} from './audio-hosts';

export function hostujePlayer(input: string): string | null {
  const id = hostujeId(input);
  return id ? `https://hostuje.net/player_embed.js.php?id=${id}` : null;
}
