export interface GenreOption {
  id: string;
  slug: string;
  label: string;
  blurb: string;
  blurbEn: string;
  about: string;
  aboutEn: string;
}

export const GENRES: GenreOption[] = [
  {id: 'CLUB', slug: 'club', label: 'Club', blurb: 'Sety i numery na parkiet.', blurbEn: 'Sets and tracks for the dance floor.', about: 'Club na imprezę z nami — sety i numery, które niosą parkiet od pierwszego bitu.', aboutEn: 'Club for a night out with us — sets and tracks that carry the floor from the first beat.'},
  {id: 'DANCE', slug: 'dance', label: 'Dance', blurb: 'Single, które niosą salę.', blurbEn: 'Singles that carry the room.', about: 'Dance, przy którym śpiewasz z nami w aucie, w kuchni i na parkiecie.', aboutEn: 'Dance to sing along with us in the car, in the kitchen and on the floor.'},
  {id: 'RETRO', slug: 'retro', label: 'Retro', blurb: 'To, co wraca na parkiet.', blurbEn: 'What comes back to the floor.', about: 'Retro, które wraca z nami na parkiet — hity, przy których bawili się wszyscy.', aboutEn: 'Retro that comes back to the floor with us — hits everyone has danced to.'},
  {id: 'HOUSE', slug: 'house', label: 'House', blurb: 'Dłuższe brzmienia i groove.', blurbEn: 'Longer sounds and groove.', about: 'House na długą noc z nami — groove, który trzyma parkiet do rana.', aboutEn: 'House for a long night with us — groove that holds the floor till morning.'},
  {id: 'VIXA', slug: 'vixa', label: 'Vixa', blurb: 'Szybkie bity i pompowane wokale.', blurbEn: 'Fast beats and pumped vocals.', about: 'Vixa, z którą rozkręcasz z nami każdą imprezę — szybkie bity i pompowane wokale.', aboutEn: 'Vixa to get any party going with us — fast beats and pumped vocals.'},
  {id: 'BASS_HOUSE', slug: 'bass-house', label: 'Bass House', blurb: 'Mocny bas i klubowy drive.', blurbEn: 'Heavy bass and a club drive.', about: 'Bass house na siłownię i parkiet — mocny bas, który pcha Cię z nami do przodu.', aboutEn: 'Bass house for the gym and the floor — heavy bass that drives you on with us.'},
  {id: 'TECHNO', slug: 'techno', label: 'Techno', blurb: 'Powtarzalny bit i nocny klimat.', blurbEn: 'A repeating beat and a late-night mood.', about: 'Techno na nocną jazdę z nami — powtarzalny bit, w którym łatwo się zatracić.', aboutEn: 'Techno for a late-night ride with us — a repeating beat that is easy to get lost in.'},
  {id: 'BIG_ROOM', slug: 'big-room', label: 'Big Room', blurb: 'Duże brzmienie i festiwalowy drop.', blurbEn: 'A big sound and a festival drop.', about: 'Big room, przy którym skaczesz z nami jak pod festiwalową sceną.', aboutEn: 'Big room to jump with us like you are in front of a festival stage.'},
  {id: 'TRANCE', slug: 'trance', label: 'Trance', blurb: 'Długie budowanie i melodyjny drive.', blurbEn: 'A long build and a melodic drive.', about: 'Trance na trening, drogę i noc z nami — długie budowanie i melodia, która niesie.', aboutEn: 'Trance for a workout, the road and the night with us — a long build and a melody that carries you.'},
  {id: 'ELECTRO', slug: 'electro', label: 'Electro', blurb: 'Syntetyczny bit i klubowa energia.', blurbEn: 'A synthetic beat and club energy.', about: 'Electro z klubową energią — włącz je z nami i podkręć tempo.', aboutEn: 'Electro with club energy — press play with us and turn up the pace.'},
  {id: 'HANDS_UP', slug: 'hands-up', label: 'Hands Up', blurb: 'Ręce w górę i melodie na refren.', blurbEn: 'Hands in the air and sing-along leads.', about: 'Hands up — refreny, przy których cała sala podnosi z nami ręce.', aboutEn: 'Hands up — choruses that get the whole room raising their hands with us.'},
  {id: 'POP', slug: 'pop', label: 'Pop', blurb: 'Radiowe hity na każdą imprezę.', blurbEn: 'Radio hits for any party.', about: 'Pop, który śpiewasz z nami — hity z radia i list przebojów, które zna każdy.', aboutEn: 'Pop to sing along with us — radio and chart hits everyone knows.'},
  {id: 'DISCO_POLO', slug: 'disco-polo', label: 'Disco Polo', blurb: 'Wesela, poprawiny i hity z parkietu.', blurbEn: 'Weddings, afterparties and floor hits.', about: 'Disco polo na wesele, poprawiny i domówkę — bawisz się z nami do białego rana.', aboutEn: 'Disco polo for weddings, afterparties and house parties — party with us till dawn.'}
];

export function genreById(id: string): GenreOption | undefined {
  const key = (id || '').trim().toUpperCase();
  return GENRES.find(genre => genre.id === key);
}

export function genreBySlug(slug: string): GenreOption | undefined {
  const key = (slug || '').trim().toLowerCase().replace(/_/g, '-');
  return GENRES.find(genre => genre.slug === key || genre.slug.replace(/-/g, '') === key);
}

export function genreLabel(id: string): string {
  return genreById(id)?.label || id || 'Utwory';
}

export function genreSlug(id: string): string {
  return genreById(id)?.slug || 'club';
}
