export function versionLabel(track: {title?: string, version?: string}): string {
  const title = (track?.title || '').trim();
  const version = (track?.version || '').trim();
  if (!version || title.toLowerCase().includes(version.toLowerCase())) {
    return '';
  }
  return version;
}

export function scoreTone(score: number): 'down' | 'flat' | 'up' | 'hot' | 'anthem' {
  const value = Number(score) || 0;
  if (value < 0) {
    return 'down';
  }
  if (value >= 20) {
    return 'anthem';
  }
  if (value >= 8) {
    return 'hot';
  }
  if (value > 0) {
    return 'up';
  }
  return 'flat';
}

export function scoreTag(score: number): string {
  const tone = scoreTone(score);
  if (tone === 'anthem') {
    return 'Hymn';
  }
  if (tone === 'hot') {
    return 'Hit';
  }
  return '';
}

export function scoreText(score: number): string {
  const value = Number(score) || 0;
  return (value > 0 ? '+' : '') + value;
}
