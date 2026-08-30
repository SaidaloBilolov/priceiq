import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Bell, X, Check } from 'lucide-react';
import { Product, Language } from '../types';
import { api } from '../services/api';
import { getTelegramUser } from '../utils/telegram';

interface PriceAlertModalProps {
  product: Product;
  onClose: () => void;
}

export const PriceAlertModal: React.FC<PriceAlertModalProps> = ({ product, onClose }) => {
  const { i18n, t } = useTranslation();
  const currentLang = (i18n.language || 'uz') as Language;
  const tgUser = getTelegramUser();

  const [targetPrice, setTargetPrice] = useState(
    Math.round(product.lowestPriceUzs * 0.95).toString()
  );
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  const getTitle = () => {
    if (currentLang === 'ru') return product.titleRu || product.titleUz;
    if (currentLang === 'en') return product.titleEn || product.titleUz;
    return product.titleUz;
  };

  const formatPrice = (amount: number) => {
    return new Intl.NumberFormat('fr-FR').format(amount).replace(/,/g, ' ');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    const num = Number(targetPrice);
    if (isNaN(num) || num <= 0) {
      setError(t('common.error'));
      return;
    }

    try {
      await api.createPriceAlert(tgUser.id, product.id, num);
      setSuccess(true);
      setTimeout(() => {
        onClose();
      }, 1500);
    } catch (err: any) {
      setError(err.message || t('common.error'));
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fadeIn">
      <div className="bg-white dark:bg-gray-800 rounded-3xl max-w-sm w-full p-6 shadow-2xl relative border border-gray-100 dark:border-gray-700">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 dark:hover:text-white p-1 rounded-lg"
        >
          <X className="w-5 h-5" />
        </button>

        {success ? (
          <div className="text-center py-6">
            <div className="w-12 h-12 bg-emerald-100 text-emerald-600 dark:bg-emerald-900/60 dark:text-emerald-400 rounded-full flex items-center justify-center mx-auto mb-3">
              <Check className="w-6 h-6" />
            </div>
            <h3 className="text-base font-bold text-gray-900 dark:text-white">
              {t('common.success')}
            </h3>
            <p className="text-xs text-gray-500 mt-1">
              {t('alerts.subtitle')}
            </p>
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 bg-blue-50 dark:bg-blue-900/40 text-blue-600 dark:text-blue-400 rounded-2xl">
                <Bell className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-sm font-extrabold text-gray-900 dark:text-white">
                  {t('product.setAlert')}
                </h3>
                <p className="text-[11px] text-gray-500 line-clamp-1">{getTitle()}</p>
              </div>
            </div>

            <div className="bg-gray-50 dark:bg-gray-900 p-3 rounded-2xl mb-4 border border-gray-100 dark:border-gray-800">
              <span className="text-[10px] text-gray-400 block font-medium">{t('product.lowestPrice')}</span>
              <span className="text-sm font-extrabold text-blue-600 dark:text-blue-400">
                {formatPrice(product.lowestPriceUzs)} {t('common.currency')}
              </span>
            </div>

            <div className="mb-5">
              <label className="block text-xs font-bold text-gray-700 dark:text-gray-300 mb-1.5">
                {t('alerts.enterTargetPrice')}
              </label>
              <input
                type="number"
                value={targetPrice}
                onChange={(e) => setTargetPrice(e.target.value)}
                className="w-full px-4 py-2.5 bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl text-sm text-gray-900 dark:text-white focus:ring-2 focus:ring-blue-500 outline-none font-bold"
                placeholder="16000000"
              />
              {error && <p className="text-xs text-rose-500 mt-1">{error}</p>}
            </div>

            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 py-2.5 bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 text-gray-700 dark:text-gray-300 rounded-xl font-bold text-xs"
              >
                {t('common.cancel')}
              </button>
              <button
                type="submit"
                className="flex-1 py-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-bold text-xs shadow-md shadow-blue-500/20"
              >
                {t('alerts.setAlert')}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};
