import React from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { ShieldCheck } from 'lucide-react';
import { getTelegramUser } from '../utils/telegram';
import { Language } from '../types';

export const Header: React.FC = () => {
  const { i18n } = useTranslation();
  const currentLang = (i18n.language || 'uz') as Language;
  const tgUser = getTelegramUser();

  const handleLangChange = (lang: Language) => {
    i18n.changeLanguage(lang);
  };

  return (
    <header className="sticky top-0 z-40 bg-white/95 dark:bg-gray-900/95 backdrop-blur-md border-b border-gray-100 dark:border-gray-800 px-4 py-3">
      <div className="max-w-4xl mx-auto flex items-center justify-between gap-2">
        {/* Logo */}
        <Link to="/" className="flex items-center gap-2">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center text-white font-black text-lg shadow-sm">
            IQ
          </div>
          <div className="flex flex-col">
            <span className="font-black text-lg tracking-tight text-gray-900 dark:text-white leading-none">
              PRICE<span className="text-blue-600 dark:text-blue-400">IQ</span>
            </span>
            <span className="text-[9px] font-bold text-gray-400 dark:text-gray-500 uppercase tracking-widest">
              Uzbekistan TMA
            </span>
          </div>
        </Link>

        {/* Right Section: Admin, Language, Profile */}
        <div className="flex items-center gap-2">
          {/* Admin Dashboard Link */}
          <Link
            to="/admin"
            className="p-1.5 bg-gray-100 dark:bg-gray-800 hover:bg-blue-50 dark:hover:bg-blue-950 text-gray-700 dark:text-gray-300 hover:text-blue-600 rounded-xl transition-all flex items-center gap-1 text-[11px] font-extrabold border border-gray-200/60 dark:border-gray-700"
            title="Admin Panel"
          >
            <ShieldCheck className="w-4 h-4 text-blue-600" />
            <span className="hidden sm:inline">Admin</span>
          </Link>

          {/* 3-Language Switcher (UZ, RU, EN) */}
          <div className="inline-flex items-center bg-gray-100 dark:bg-gray-800 p-1 rounded-xl border border-gray-200/60 dark:border-gray-700">
            <button
              type="button"
              onClick={() => handleLangChange('uz')}
              className={`px-2 py-1 text-[11px] font-extrabold rounded-lg transition-all ${
                currentLang === 'uz'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-gray-600 dark:text-gray-400 hover:text-gray-900'
              }`}
            >
              🇺🇿 UZ
            </button>
            <button
              type="button"
              onClick={() => handleLangChange('ru')}
              className={`px-2 py-1 text-[11px] font-extrabold rounded-lg transition-all ${
                currentLang === 'ru'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-gray-600 dark:text-gray-400 hover:text-gray-900'
              }`}
            >
              🇷🇺 RU
            </button>
            <button
              type="button"
              onClick={() => handleLangChange('en')}
              className={`px-2 py-1 text-[11px] font-extrabold rounded-lg transition-all ${
                currentLang === 'en'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-gray-600 dark:text-gray-400 hover:text-gray-900'
              }`}
            >
              🇬🇧 EN
            </button>
          </div>

          {/* Telegram User Avatar */}
          <Link to="/profile" className="flex items-center gap-1.5 p-1 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors">
            <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-indigo-500 to-purple-600 text-white font-bold text-xs flex items-center justify-center shadow">
              {tgUser.first_name?.charAt(0).toUpperCase() || 'U'}
            </div>
          </Link>
        </div>
      </div>
    </header>
  );
};
