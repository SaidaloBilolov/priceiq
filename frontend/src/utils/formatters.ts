import { Language } from '../types';

export const formatPrice = (amount: number, lang: Language): string => {
  if (typeof amount !== 'number' || isNaN(amount)) return `0 ${lang === 'ru' ? 'сум' : 'so‘m'}`;

  const formattedNumber = new Intl.NumberFormat('fr-FR', {
    maximumFractionDigits: 0
  }).format(amount).replace(/,/g, ' ');

  const currencyUnit = lang === 'ru' ? 'сум' : 'so‘m';
  return `${formattedNumber} ${currencyUnit}`;
};

export const formatDate = (dateStr: string, lang: Language): string => {
  if (!dateStr) return '';
  try {
    const date = new Date(dateStr);
    const locale = lang === 'ru' ? 'ru-RU' : 'uz-UZ';
    return new Intl.DateTimeFormat(locale, {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    }).format(date);
  } catch {
    return dateStr;
  }
};
