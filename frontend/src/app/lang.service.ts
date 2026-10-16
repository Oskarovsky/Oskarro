import {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs';

export type LangCode = 'pl' | 'en';

const STORAGE_KEY = 'oskarro-lang';

const KNOWN: Record<string, string> = {
  'Słuchacz': 'Listener',
  'Bywalec': 'Regular',
  'Rezydent': 'Resident',
  'Selekcjoner': 'Selector',
  'Didżej': 'DJ',
  'Gwiazda': 'Star',
  'Legenda': 'Legend',
  'Hymn': 'Anthem',
  'Hit': 'Hit',
  'Dodany utwór daje 5 pkt. Napisany komentarz daje 2 pkt. Polubienie zebrane przy Twoim utworze daje 1 pkt.':
    'An added track is worth 5 points. A comment you write is worth 2. A like on your track is worth 1.',
  'Start. Wystarczy konto.': 'The start. An account is enough.',
  '500 punktów. Na przykład 100 dodanych utworów albo 250 komentarzy.':
    '500 points. For example 100 added tracks, or 250 comments.',
  '1500 punktów. Na przykład 300 utworów albo 200 utworów i 250 komentarzy.':
    '1,500 points. For example 300 tracks, or 200 tracks and 250 comments.',
  '3500 punktów. Na przykład 700 utworów, albo mniej, gdy dochodzą komentarze i polubienia.':
    '3,500 points. For example 700 tracks, or fewer when comments and likes add up.',
  '7000 punktów. Na przykład 1000 utworów i długa seria komentarzy.':
    '7,000 points. For example 1,000 tracks and a long run of comments.',
  '12000 punktów. Parkiet już Cię zna.': '12,000 points. The dance floor already knows you.',
  '20000 punktów. Najwyższa ranga, zbierana utworami, komentarzami i polubieniami.':
    '20,000 points. The top rank, built from tracks, comments and likes.',
  'Ustawienie zapisane.': 'Setting saved.',
  'Nie udało się zapisać ustawienia.': 'The setting could not be saved.',
  'Zapisane.': 'Saved.',
  'Strona musi zaczynać się od https://': 'The site must start with https://',
  'Hasło musi mieć od 6 znaków i być powtórzone tak samo.': 'The password needs at least 6 characters and the same repeat.',
  'Hasło zmienione.': 'Password changed.',
  'Nie udało się zmienić hasła.': 'The password could not be changed.',
  'Nie udało się zapisać opisu.': 'The bio could not be saved.',
  'Ranga przyznana.': 'Rank granted.',
  'Ranga wróciła do aktywności.': 'The rank went back to activity.',
  'Nie udało się zmienić rangi.': 'The rank could not be changed.',
  'Zdjęcie zapisane.': 'Photo saved.',
  'Nie udało się wgrać zdjęcia.': 'The photo could not be uploaded.',
  'Zdjęcie może mieć najwyżej 2 MB.': 'The photo can be at most 2 MB.',
  'Podaj wykonawcę i tytuł.': 'Enter the artist and the title.',
  'Ten link jest z soundfiles.eu. Zmień źródło pliku.': 'This link is from soundfiles.eu. Change the file source.',
  'Podaj link hostuje.net, na przykład https://hostuje.net/file.php?id= i 32 znaki.':
    'Enter a hostuje.net link, for example https://hostuje.net/file.php?id= and 32 characters.',
  'Ten link jest z hostuje.net. Zmień źródło pliku.': 'This link is from hostuje.net. Change the file source.',
  'Podaj link soundfiles.eu, na przykład https://soundfiles.eu/QN9mExDZG6dW/file.':
    'Enter a soundfiles.eu link, for example https://soundfiles.eu/QN9mExDZG6dW/file.',
  'Dodaj zdjęcie utworu. Zapisujemy z niego miniaturę.': 'Add a track photo. A thumbnail is saved from it.',
  'Nie udało się zapisać. Poprawki są dostępne tylko dla autora przez 24 godziny.':
    'Could not save. Edits are open only to the author for 24 hours.',
  'Nie udało się dodać utworu.': 'The track could not be added.',
  'Dane są zapisane, ale zdjęcia nie udało się zmniejszyć. Możesz spróbować ponownie na stronie poprawki.':
    'The details are saved, but the photo could not be resized. You can try again on the edit page.',
  'Nazwa musi mieć przynajmniej 2 znaki.': 'The name needs at least 2 characters.',
  'Nie udało się utworzyć składanki. Zaloguj się i spróbuj jeszcze raz.':
    'The playlist could not be created. Sign in and try again.',
  'Strony utworów': 'Track pages',
  'Strony setów': 'Set pages',
  'Utwory': 'Tracks',
  'Utwór': 'Track',
  'Nieznany wykonawca': 'Unknown artist',
  'Składanka': 'Playlist'
};

@Injectable({providedIn: 'root'})
export class LangService {
  private readonly state = new BehaviorSubject<LangCode>(this.read());
  readonly changes = this.state.asObservable();

  constructor() {
    document.documentElement.lang = this.code;
  }

  get code(): LangCode {
    return this.state.value;
  }

  get en(): boolean {
    return this.code === 'en';
  }

  use(code: LangCode) {
    if (code !== 'pl' && code !== 'en' || code === this.code) {
      return;
    }
    localStorage.setItem(STORAGE_KEY, code);
    document.documentElement.lang = code;
    this.state.next(code);
    window.location.reload();
  }

  t(pl: string, en: string): string {
    return this.en ? en : pl;
  }

  text(value: string): string {
    if (!this.en || !value) {
      return value || '';
    }
    return KNOWN[value] || value;
  }

  plural(count: number, pl: [string, string, string], en: [string, string]): string {
    const value = Number(count) || 0;
    if (this.en) {
      return value === 1 ? en[0] : en[1];
    }
    const mod10 = value % 10;
    const mod100 = value % 100;
    if (mod10 === 1 && mod100 !== 11) {
      return pl[0];
    }
    if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
      return pl[1];
    }
    return pl[2];
  }

  private read(): LangCode {
    try {
      return localStorage.getItem(STORAGE_KEY) === 'en' ? 'en' : 'pl';
    } catch {
      return 'pl';
    }
  }
}
