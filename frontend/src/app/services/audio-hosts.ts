const HOSTUJE_ID = '([a-f0-9]{32})';
const SOUNDFILES_ID = '([A-Za-z0-9]{12})';

export type AudioHost = 'hostuje' | 'soundfiles';

export interface AudioRef {
  host: AudioHost;
  id: string;
}

export function hostujeId(input: string): string | null {
  const value = unwrap(input);
  if (!value) {
    return null;
  }
  const fromUrl = value.match(new RegExp(
    '(?:https?:\\/\\/)?(?:www\\.)?hostuje\\.net/(?:file\\.php\\?id=|player_embed\\.js\\.php\\?id=)' + HOSTUJE_ID, 'i'));
  const id = fromUrl ? fromUrl[1] : (value.match(new RegExp('^' + HOSTUJE_ID + '$', 'i')) || [])[1];
  return id ? id.toLowerCase() : null;
}

export function soundfilesId(input: string): string | null {
  const value = unwrap(input);
  if (!value) {
    return null;
  }
  const fromUrl = value.match(new RegExp(
    '(?:https?:\\/\\/)?(?:www\\.)?soundfiles\\.eu\\/' + SOUNDFILES_ID + '(?:\\/(?:file(?:\\/(?:download|stream))?)?)?(?:\\?[^\\s]*)?',
    'i'));
  if (fromUrl) {
    return fromUrl[1];
  }
  const bare = value.match(new RegExp('^' + SOUNDFILES_ID + '$'));
  return bare ? bare[1] : null;
}

export function audioRef(input: string): AudioRef | null {
  const hostuje = hostujeId(input);
  if (hostuje) {
    return {host: 'hostuje', id: hostuje};
  }
  const soundfiles = soundfilesId(input);
  return soundfiles ? {host: 'soundfiles', id: soundfiles} : null;
}

export function audioHostOf(input: string): AudioHost {
  return hostujeId(input) ? 'hostuje' : 'soundfiles';
}

export function canonicalAudioUrl(input: string): string | null {
  const ref = audioRef(input);
  if (!ref) {
    return null;
  }
  return ref.host === 'hostuje'
    ? 'https://hostuje.net/file.php?id=' + ref.id
    : 'https://soundfiles.eu/' + ref.id + '/file';
}

function unwrap(input: string): string {
  if (!input) {
    return '';
  }
  let value = input.trim();
  const embedded = value.match(/src=["']([^"']+)["']/i);
  if (embedded) {
    value = embedded[1].trim();
  }
  return value;
}
