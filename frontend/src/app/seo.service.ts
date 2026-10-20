import {DOCUMENT} from '@angular/common';
import {Inject, Injectable} from '@angular/core';
import {Meta, Title} from '@angular/platform-browser';
import {genreBySlug, genreLabel} from './tracks/genres';
import {LangService} from './lang.service';

const ORIGIN = 'https://oskarro.com';
const LOGO = ORIGIN + '/assets/logo.jpg';
const HOME_TITLE = 'Muzyka klubowa, vixa i disco polo za darmo — słuchaj z nami | Oskarro';
const HOME_DESCRIPTION = 'Oskarro.com — słuchaj z nami muzyki klubowej za darmo: club, vixa, trance, hands up i disco polo. Oceniaj, komentuj i dodawaj swoje numery.';
const HOME_DESCRIPTION_EN = 'Oskarro.com — listen with us to club music for free: club, vixa, trance, hands up and disco polo. Vote, comment and share your own tracks.';

@Injectable({providedIn: 'root'})
export class SeoService {

  constructor(private title: Title,
              private meta: Meta,
              @Inject(DOCUMENT) private document: Document,
              private lang: LangService) {}

  applyRoute(url: string) {
    const path = this.pathOf(url);
    const query = new URLSearchParams(url.includes('?') ? url.slice(url.indexOf('?') + 1) : '');

    if (path === '/') {
      const description = this.lang.t(HOME_DESCRIPTION, HOME_DESCRIPTION_EN);
      this.set(this.lang.t(HOME_TITLE, 'Listen with us | Oskarro'), description, '/');
      this.jsonLd({
        '@context': 'https://schema.org',
        '@type': 'WebSite',
        name: 'Oskarro',
        url: ORIGIN + '/',
        description: description,
        inLanguage: this.lang.code,
        potentialAction: {
          '@type': 'SearchAction',
          target: ORIGIN + '/szukaj?q={search_term_string}',
          'query-input': 'required name=search_term_string'
        }
      });
      return;
    }

    const genreSlug = path.match(/^\/tracklist\/([a-z0-9-]+)$/);
    if (genreSlug) {
      const genre = genreBySlug(genreSlug[1]);
      if (!genre) {
        this.set(this.lang.t('Utwory | Oskarro', 'Tracks | Oskarro'), this.lang.t(HOME_DESCRIPTION, HOME_DESCRIPTION_EN), path, false);
        return;
      }
      const about = this.lang.en ? genre.aboutEn : genre.about;
      const heading = genre.id === 'CLUB'
        ? this.lang.t('Muzyka klubowa — Club | Oskarro', 'Club music — Club | Oskarro')
        : genre.label + this.lang.t(' na imprezę | Oskarro', ' for a party | Oskarro');
      this.set(heading, about, path);
      this.jsonLd({
        '@context': 'https://schema.org',
        '@type': 'CollectionPage',
        name: heading,
        description: about,
        url: ORIGIN + path,
        inLanguage: this.lang.code,
        isPartOf: {'@type': 'WebSite', name: 'Oskarro', url: ORIGIN + '/'}
      });
      return;
    }

    if (/^\/track\/\d+$/.test(path)) {
      this.set(this.lang.t('Utwór | Oskarro', 'Track | Oskarro'), this.lang.t('Numer z Oskarro. Muzyka klubowa i muzyka na imprezę.', 'A track from Oskarro. Club music and party music.'), path);
      return;
    }

    if (path === '/video') {
      this.set(this.lang.t('Sety na imprezę | Oskarro', 'Party sets | Oskarro'), this.lang.t('Sety Oskarro na imprezę, do auta i na parkiet. Mixy klubowe z kanału.', 'Oskarro sets for a party, the car and the dance floor. Club mixes from the channel.'), path);
      return;
    }

    if (/^\/video\/set\/[A-Za-z0-9_-]{11}$/.test(path)) {
      this.set(this.lang.t('Set na imprezę | Oskarro', 'Party set | Oskarro'), this.lang.t('Set Oskarro. Muzyka klubowa na imprezę, do auta i na parkiet.', 'An Oskarro set. Club music for a party, the car and the dance floor.'), path);
      return;
    }

    if (path === '/playlist/all') {
      this.set(this.lang.t('Playlisty na imprezę | Oskarro', 'Party playlists | Oskarro'), this.lang.t('Składanki na imprezę ułożone przez ludzi z Oskarro.', 'Party playlists put together by people on Oskarro.'), path);
      return;
    }

    if (/^\/playlist\/\d+\/details$/.test(path)) {
      this.set(this.lang.t('Składanka na imprezę | Oskarro', 'Party playlist | Oskarro'), this.lang.t('Składanka z Oskarro. Muzyka na imprezę i na parkiet.', 'A playlist from Oskarro. Music for a party and the dance floor.'), path);
      return;
    }

    if (path === '/ranking') {
      this.set('Ranking | Oskarro', this.lang.t('Kto dodaje muzykę klubową i numery na imprezę na Oskarro.', 'Who adds club music and party tracks on Oskarro.'), path);
      return;
    }

    if (path === '/radio') {
      this.set('Radio | Oskarro', this.lang.t('Włącz radio Oskarro i słuchaj muzyki klubowej na imprezę. Kolejny utwór startuje sam.', 'Start Oskarro radio and listen to club music for a party. The next track starts on its own.'), path);
      return;
    }

    if (path === '/szukaj') {
      const q = (query.get('q') || '').trim();
      this.set(
        q ? this.lang.t('Szukaj: ', 'Search: ') + q + ' | Oskarro' : this.lang.t('Szukaj utworu | Oskarro', 'Search for a track | Oskarro'),
        this.lang.t('Szukaj muzyki klubowej i numerów na imprezę.', 'Search club music and party tracks.'),
        q ? '/szukaj?q=' + encodeURIComponent(q) : '/szukaj',
        false
      );
      return;
    }

    this.set('Oskarro', this.lang.t(HOME_DESCRIPTION, HOME_DESCRIPTION_EN), path, !this.privatePath(path));
  }

  /** Same title and description as the server-rendered page (SeoPageController). */
  track(track: {id: number; title: string; artist: string; genre: string; version?: string; kind?: string; hasCover?: boolean}) {
    const name = (track.title || this.lang.t('Bez tytułu', 'Untitled')).trim();
    const artist = (track.artist || this.lang.t('Nieznany wykonawca', 'Unknown artist')).trim();
    const version = (track.version || '').trim() ? ' (' + track.version.trim() + ')' : '';
    const genre = genreLabel(track.genre);
    const path = '/track/' + track.id;
    const full = artist + ' – ' + name + version;
    const set = (track.kind || '').toLowerCase() === 'set';
    const description = this.lang.en
      ? 'Listen for free: ' + full + '. ' + genre + ' on Oskarro. Vote, comment and save it to your playlist.'
      : (set ? 'Posłuchaj seta za darmo: ' : 'Posłuchaj za darmo: ') + full + '. ' + genre + ' na Oskarro. Oceniaj, komentuj i zapisz do swojej składanki.';
    const image = track.hasCover ? ORIGIN + '/api/tracks/' + track.id + '/cover' : LOGO;
    this.set(full + ' | Oskarro', description, path, true, image);
    this.jsonLd({
      '@context': 'https://schema.org',
      '@type': 'MusicRecording',
      name: name + version,
      byArtist: {'@type': 'MusicGroup', name: artist},
      genre,
      url: ORIGIN + path,
      image,
      inLanguage: 'pl'
    });
  }

  playlist(playlist: {id: number; name: string; description?: string}) {
    const name = (playlist.name || this.lang.t('Składanka', 'Playlist')).trim();
    const description = (playlist.description || '').trim() || name + this.lang.t('. Składanka na imprezę z Oskarro.', '. A party playlist from Oskarro.');
    this.set(name + ' | Oskarro', description, '/playlist/' + playlist.id + '/details');
  }

  setTitle(title: string, description: string, path: string) {
    this.set(title, description, path);
  }

  private set(title: string, description: string, path: string, index = true, image = LOGO) {
    this.title.setTitle(title);
    this.meta.updateTag({name: 'description', content: description});
    this.meta.updateTag({name: 'robots', content: index ? 'index, follow' : 'noindex, follow'});
    this.meta.updateTag({property: 'og:title', content: title});
    this.meta.updateTag({property: 'og:description', content: description});
    this.meta.updateTag({property: 'og:locale', content: this.lang.en ? 'en_US' : 'pl_PL'});
    this.meta.updateTag({property: 'og:url', content: ORIGIN + path});
    this.meta.updateTag({property: 'og:image', content: image});
    this.meta.updateTag({name: 'twitter:title', content: title});
    this.meta.updateTag({name: 'twitter:description', content: description});
    this.meta.updateTag({name: 'twitter:image', content: image});
    let link = this.document.querySelector('link[rel="canonical"]') as HTMLLinkElement | null;
    if (!link) {
      link = this.document.createElement('link');
      link.setAttribute('rel', 'canonical');
      this.document.head.appendChild(link);
    }
    link.setAttribute('href', ORIGIN + path);
  }

  private jsonLd(data: object) {
    let script = this.document.getElementById('oskarro-ld');
    if (!script) {
      script = this.document.createElement('script');
      script.id = 'oskarro-ld';
      script.setAttribute('type', 'application/ld+json');
      this.document.head.appendChild(script);
    }
    script.textContent = JSON.stringify(data).replace(/</g, '\\u003c');
  }

  private pathOf(url: string) {
    const bare = url.split('?')[0].split('#')[0];
    if (bare.length > 1 && bare.endsWith('/')) {
      return bare.slice(0, -1);
    }
    return bare || '/';
  }

  private privatePath(path: string) {
    return path === '/login'
      || path === '/register'
      || path === '/przypomnij'
      || path === '/haslo'
      || path === '/profile'
      || path.startsWith('/profile/')
      || path.startsWith('/admin')
      || path.startsWith('/mod')
      || path.startsWith('/user')
      || path.startsWith('/toplist')
      || path.endsWith('/add')
      || path.endsWith('/edit');
  }
}
