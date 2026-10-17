export interface SetCategoryOption {
  id: 'USERS' | 'CLUBS' | 'FESTIVALS' | 'RADIO';
  slug: string;
  label: string;
  labelEn: string;
  blurb: string;
  blurbEn: string;
  eventLabel: string;
  eventLabelEn: string;
  art: string;
  palette: [string, string];
}

/** Sub-sections of "Sety/Mixy"; ids match the backend SetCategory enum, slugs are the URLs. */
export const SET_CATEGORIES: SetCategoryOption[] = [
  {id: 'USERS', slug: 'uzytkownicy', label: 'Sety użytkowników', labelEn: 'User sets',
    blurb: 'Miksy nagrane przez didżejów z naszej społeczności.', blurbEn: 'Mixes recorded by DJs from our community.',
    eventLabel: 'Nazwa seta lub okazja (opcjonalnie)', eventLabelEn: 'Set name or occasion (optional)',
    art: 'HOUSE', palette: ['#3a1c71', '#d76d77']},
  {id: 'CLUBS', slug: 'kluby', label: 'Kluby', labelEn: 'Clubs',
    blurb: 'Sety nagrane na żywo w klubach.', blurbEn: 'Sets recorded live in clubs.',
    eventLabel: 'Klub', eventLabelEn: 'Club',
    art: 'CLUB', palette: ['#0f0c29', '#e94057']},
  {id: 'FESTIVALS', slug: 'festiwale', label: 'Festiwale', labelEn: 'Festivals',
    blurb: 'Występy z dużych scen i festiwali.', blurbEn: 'Performances from big stages and festivals.',
    eventLabel: 'Festiwal', eventLabelEn: 'Festival',
    art: 'BIG_ROOM', palette: ['#1f1c2c', '#f7971e']},
  {id: 'RADIO', slug: 'audycje', label: 'Audycje radiowe', labelEn: 'Radio shows',
    blurb: 'Nagrania audycji i radiowych mixów.', blurbEn: 'Recorded radio shows and radio mixes.',
    eventLabel: 'Audycja / stacja', eventLabelEn: 'Show / station',
    art: 'TRANCE', palette: ['#134e5e', '#71b280']}
];

export function setCategoryBySlug(slug: string): SetCategoryOption | undefined {
  const key = (slug || '').trim().toLowerCase();
  return SET_CATEGORIES.find(c => c.slug === key || c.id.toLowerCase() === key);
}

export function setCategoryById(id: string): SetCategoryOption | undefined {
  return SET_CATEGORIES.find(c => c.id === (id || '').trim().toUpperCase());
}
