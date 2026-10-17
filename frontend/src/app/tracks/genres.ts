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
  {id: 'CLUB', slug: 'club', label: 'Club', blurb: 'Sety i numery na parkiet.', blurbEn: 'Sets and tracks for the dance floor.', about: 'Muzyka klubowa na imprezę: sety i numery, które niosą parkiet.', aboutEn: 'Club music for a party: sets and tracks that carry the dance floor.'},
  {id: 'DANCE', slug: 'dance', label: 'Dance', blurb: 'Single, które niosą salę.', blurbEn: 'Singles that carry the room.', about: 'Dance na imprezę. Single, które niosą salę od pierwszego refrenu.', aboutEn: 'Dance for a party. Singles that carry the room from the first chorus.'},
  {id: 'RETRO', slug: 'retro', label: 'Retro', blurb: 'To, co wraca na parkiet.', blurbEn: 'What comes back to the floor.', about: 'Retro na imprezę. Numery, które wracają na parkiet.', aboutEn: 'Retro for a party. Tracks that return to the dance floor.'},
  {id: 'HOUSE', slug: 'house', label: 'House', blurb: 'Dłuższe brzmienia i groove.', blurbEn: 'Longer sounds and groove.', about: 'House na dłuższą imprezę. Groove, który trzyma parkiet.', aboutEn: 'House for a longer party. Groove that holds the dance floor.'},
  {id: 'VIXA', slug: 'vixa', label: 'Vixa', blurb: 'Szybkie bity i pompowane wokale.', blurbEn: 'Fast beats and pumped vocals.', about: 'Vixa na imprezę. Szybkie bity i pompowane wokale.', aboutEn: 'Vixa for a party. Fast beats and pumped vocals.'},
  {id: 'BASS_HOUSE', slug: 'bass-house', label: 'Bass House', blurb: 'Mocny bas i klubowy drive.', blurbEn: 'Heavy bass and a club drive.', about: 'Bass house na parkiet. Mocny bas i klubowy drive.', aboutEn: 'Bass house for the floor. Heavy bass and a club drive.'},
  {id: 'TECHNO', slug: 'techno', label: 'Techno', blurb: 'Powtarzalny bit i nocny klimat.', blurbEn: 'A repeating beat and a late-night mood.', about: 'Techno na nocną imprezę. Powtarzalny bit i klubowy klimat.', aboutEn: 'Techno for a late-night party. A repeating beat and a club mood.'},
  {id: 'BIG_ROOM', slug: 'big-room', label: 'Big Room', blurb: 'Duże brzmienie i festiwalowy drop.', blurbEn: 'A big sound and a festival drop.', about: 'Big room na imprezę. Duże brzmienie i festiwalowy drop.', aboutEn: 'Big room for a party. A big sound and a festival drop.'},
  {id: 'TRANCE', slug: 'trance', label: 'Trance', blurb: 'Długie budowanie i melodyjny drive.', blurbEn: 'A long build and a melodic drive.', about: 'Trance na imprezę. Długie budowanie i melodyjny drive.', aboutEn: 'Trance for a party. A long build and a melodic drive.'},
  {id: 'ELECTRO', slug: 'electro', label: 'Electro', blurb: 'Syntetyczny bit i klubowa energia.', blurbEn: 'A synthetic beat and club energy.', about: 'Electro na parkiet. Syntetyczny bit i klubowa energia.', aboutEn: 'Electro for the floor. A synthetic beat and club energy.'},
  {id: 'HANDS_UP', slug: 'hands-up', label: 'Hands Up', blurb: 'Ręce w górę i melodie na refren.', blurbEn: 'Hands in the air and sing-along leads.', about: 'Hands up na imprezę. Szybki bit, melodyjne leady i refreny, przy których cała sala podnosi ręce.', aboutEn: 'Hands up for a party. A fast beat, melodic leads and choruses that lift every hand in the room.'},
  {id: 'POP', slug: 'pop', label: 'Pop', blurb: 'Radiowe hity na każdą imprezę.', blurbEn: 'Radio hits for any party.', about: 'Pop na imprezę. Hity z radia i list przebojów, które zna każdy na sali.', aboutEn: 'Pop for a party. Radio and chart hits that everyone in the room knows.'},
  {id: 'DISCO_POLO', slug: 'disco-polo', label: 'Disco Polo', blurb: 'Wesela, poprawiny i hity z parkietu.', blurbEn: 'Weddings, afterparties and floor hits.', about: 'Disco polo na wesele, poprawiny i imprezę. Hity z parkietu.', aboutEn: 'Disco polo for a wedding, an afterparty and a night out. Hits from the dance floor.'}
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
