import React from 'react';
import { useTranslation } from 'react-i18next';
import { Language } from '../types';

interface LanguageSwitcherProps {
  variant?: 'desktop' | 'mobile' | 'compact';
  onLanguageChange?: (lang: Language) => void;
}

export const LanguageSwitcher: React.FC<LanguageSwitcherProps> = ({
  variant = 'desktop',
  onLanguageChange
}) => {
  const { i18n } = useTranslation();
  const currentLang = (i18n.language || 'uz').toLowerCase().startsWith('ru') ? 'ru' : 'uz';

  const handleSelectLanguage = (lang: Language) => {
    if (lang === currentLang) return;
    i18n.changeLanguage(lang);
    if (onLanguageChange) {
      onLanguageChange(lang);
    }
  };

  if (variant === 'mobile') {
    return (
      <div className="flex items-center justify-between p-3 bg-gray-50 rounded-xl dark:bg-gray-800 border border-gray-200 dark:border-gray-700">
        <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
          {currentLang === 'uz' ? 'Til / Язык' : 'Язык / Til'}
        </span>
        <div className="flex items-center space-x-1 bg-gray-200 dark:bg-gray-700 p-1 rounded-lg">
          <button
            type="button"
            onClick={() => handleSelectLanguage('uz')}
            className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-all duration-200 flex items-center gap-1.5 ${
              currentLang === 'uz'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'text-gray-600 dark:text-gray-300 hover:text-gray-900'
            }`}
          >
            <span>🇺🇿</span>
            <span>O‘zbek</span>
          </button>
          <button
            type="button"
            onClick={() => handleSelectLanguage('ru')}
            className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-all duration-200 flex items-center gap-1.5 ${
              currentLang === 'ru'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'text-gray-600 dark:text-gray-300 hover:text-gray-900'
            }`}
          >
            <span>🇷🇺</span>
            <span>Русский</span>
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="inline-flex items-center bg-gray-100 dark:bg-gray-800 p-1 rounded-lg border border-gray-200 dark:border-gray-700 shadow-inner">
      <button
        type="button"
        onClick={() => handleSelectLanguage('uz')}
        className={`px-2.5 py-1 text-xs font-bold rounded-md flex items-center gap-1.5 transition-all duration-200 ${
          currentLang === 'uz'
            ? 'bg-white dark:bg-gray-900 text-blue-600 dark:text-blue-400 shadow-sm border border-gray-200/60 dark:border-gray-700'
            : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
        }`}
        aria-label="Switch to Uzbek"
      >
        <span>🇺🇿</span>
        <span>UZ</span>
      </button>
      <button
        type="button"
        onClick={() => handleSelectLanguage('ru')}
        className={`px-2.5 py-1 text-xs font-bold rounded-md flex items-center gap-1.5 transition-all duration-200 ${
          currentLang === 'ru'
            ? 'bg-white dark:bg-gray-900 text-blue-600 dark:text-blue-400 shadow-sm border border-gray-200/60 dark:border-gray-700'
            : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'
        }`}
        aria-label="Switch to Russian"
      >
        <span>🇷🇺</span>
        <span>RU</span>
      </button>
    </div>
  );
};
