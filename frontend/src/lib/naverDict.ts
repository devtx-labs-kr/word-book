/** Naver English dictionary search URL for a word (BR-DICT-1). */
export function naverDictUrl(word: string): string {
  return `https://dict.naver.com/enkodict/#/search?query=${encodeURIComponent(word)}`;
}
