import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import uzTranslations from './locales/uz.json';
import ruTranslations from './locales/ru.json';
import enTranslations from './locales/en.json';

const STORAGE_KEY = 'priceiq_tma_lang';

const savedLanguage = typeof window !== 'undefined' ? localStorage.getItem(STORAGE_KEY) : null;
const initialLanguage = savedLanguage && ['uz', 'ru', 'en'].includes(savedLanguage) ? savedLanguage : 'uz';

i18n
  .use(initReactI18next)
  .init({
    resources: {
      uz: { translation: uzTranslations },
      ru: { translation: ruTranslations },
      en: { translation: enTranslations }
    },
    lng: initialLanguage,
    fallbackLng: 'uz',
    interpolation: {
      escapeValue: false
    }
  });

i18n.on('languageChanged', (lng) => {
  if (typeof window !== 'undefined') {
    localStorage.setItem(STORAGE_KEY, lng);
  }
});

export default i18n;
