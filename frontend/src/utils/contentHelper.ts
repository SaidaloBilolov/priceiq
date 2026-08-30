import { Language } from '../types';

export function getLocalizedName(
  item?: { name_uz?: string; name_ru?: string } | null,
  lang: Language = 'uz'
): string {
  if (!item) return lang === 'ru' ? 'Без названия' : 'Nomsiz';

  const nameUz = item.name_uz?.trim();
  const nameRu = item.name_ru?.trim();

  if (lang === 'uz') {
    return nameUz || nameRu || 'Nomsiz';
  } else {
    return nameRu || nameUz || 'Без названия';
  }
}

export function getLocalizedDescription(
  item?: { description_uz?: string; description_ru?: string } | null,
  lang: Language = 'uz'
): string {
  if (!item) return '';

  const descUz = item.description_uz?.trim();
  const descRu = item.description_ru?.trim();

  if (lang === 'uz') {
    return descUz || descRu || '';
  } else {
    return descRu || descUz || '';
  }
}
