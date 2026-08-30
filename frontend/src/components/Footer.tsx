import React from 'react';
import { useTranslation } from 'react-i18next';
import { ShieldCheck, RefreshCw, BarChart2 } from 'lucide-react';

export const Footer: React.FC = () => {
  const { t } = useTranslation();

  return (
    <footer className="bg-gray-900 text-gray-400 border-t border-gray-800 mt-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div className="col-span-1 md:col-span-2">
            <div className="flex items-center gap-2 mb-4">
              <div className="w-8 h-8 rounded-lg bg-blue-600 text-white font-black flex items-center justify-center text-base">
                IQ
              </div>
              <span className="font-black text-xl text-white">PRICEIQ</span>
            </div>
            <p className="text-sm text-gray-400 max-w-sm">
              {t('home.subtitle')}
            </p>
          </div>

          <div>
            <h4 className="text-sm font-bold text-white uppercase tracking-wider mb-4">
              {t('navigation.home')}
            </h4>
            <ul className="space-y-2 text-sm">
              <li><a href="#" className="hover:text-white transition-colors">{t('home.recommended')}</a></li>
              <li><a href="#" className="hover:text-white transition-colors">{t('home.dailyDeals')}</a></li>
              <li><a href="#" className="hover:text-white transition-colors">{t('home.categories')}</a></li>
            </ul>
          </div>

          <div>
            <h4 className="text-sm font-bold text-white uppercase tracking-wider mb-4">
              {t('common.appName')}
            </h4>
            <ul className="space-y-2 text-sm">
              <li><a href="#" className="hover:text-white transition-colors">{t('product.comparePrices')}</a></li>
              <li><a href="#" className="hover:text-white transition-colors">{t('navigation.alerts')}</a></li>
              <li><a href="#" className="hover:text-white transition-colors">{t('navigation.favorites')}</a></li>
            </ul>
          </div>
        </div>

        <div className="mt-12 pt-8 border-t border-gray-800 text-xs text-center text-gray-500">
          &copy; {new Date().getFullYear()} PRICEIQ Uzbekistan. All rights reserved. 🇺🇿 O‘zbekiston do‘konlari narxlar monitorig tizimi.
        </div>
      </div>
    </footer>
  );
};
