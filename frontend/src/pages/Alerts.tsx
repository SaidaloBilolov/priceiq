import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Bell, Trash2, Smartphone } from 'lucide-react';
import { PriceAlert, Language } from '../types';
import { api } from '../services/api';
import { getTelegramUser } from '../utils/telegram';

export const AlertsPage: React.FC = () => {
  const { i18n, t } = useTranslation();
  const tgUser = getTelegramUser();
  const currentLang = (i18n.language || 'uz') as Language;

  const [alerts, setAlerts] = useState<PriceAlert[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchAlerts = async () => {
    setLoading(true);
    try {
      const data = await api.getPriceAlerts(tgUser.id);
      setAlerts(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAlerts();
  }, []);

  const handleDelete = async (alertId: number) => {
    try {
      await api.deletePriceAlert(alertId);
      setAlerts((prev) => prev.filter((a) => a.id !== alertId));
    } catch (err) {
      console.error(err);
    }
  };

  const getTitle = (alert: PriceAlert) => {
    if (!alert.product) return '';
    if (currentLang === 'ru') return alert.product.titleRu || alert.product.titleUz;
    if (currentLang === 'en') return alert.product.titleEn || alert.product.titleUz;
    return alert.product.titleUz;
  };

  const formatPrice = (amount: number) => {
    return new Intl.NumberFormat('fr-FR').format(amount).replace(/,/g, ' ');
  };

  return (
    <div className="space-y-6 pb-20">
      <div>
        <h1 className="text-xl font-black text-gray-900 dark:text-white uppercase tracking-wider flex items-center gap-2">
          <Bell className="w-5 h-5 text-blue-600 fill-current" />
          <span>{t('alerts.title')}</span>
        </h1>
        <p className="text-xs text-gray-500 mt-1">{t('alerts.subtitle')}</p>
      </div>

      {loading ? (
        <div className="flex items-center justify-center min-h-[200px]">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      ) : alerts.length === 0 ? (
        <div className="text-center py-16 bg-white dark:bg-gray-800 rounded-3xl border border-gray-100 dark:border-gray-700">
          <Bell className="w-12 h-12 text-gray-300 mx-auto mb-3" />
          <p className="text-sm font-bold text-gray-500">{t('alerts.noAlerts')}</p>
        </div>
      ) : (
        <div className="space-y-3">
          {alerts.map((alert) => (
            <div
              key={alert.id}
              className="bg-white dark:bg-gray-800 p-4 rounded-3xl border border-gray-100 dark:border-gray-700/60 shadow-sm flex items-center justify-between gap-3"
            >
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 bg-gray-50 dark:bg-gray-900 rounded-2xl p-1.5 border border-gray-100 flex items-center justify-center shrink-0">
                  <img
                    src={alert.product?.imageUrl}
                    alt={getTitle(alert)}
                    className="w-full h-full object-contain"
                  />
                </div>
                <div>
                  <h3 className="text-xs font-bold text-gray-900 dark:text-white line-clamp-1">
                    {getTitle(alert)}
                  </h3>
                  <div className="flex items-center gap-2 text-[10px] mt-1">
                    <span className="text-gray-400 font-medium">{t('alerts.targetPrice')}:</span>
                    <span className="font-extrabold text-blue-600 dark:text-blue-400">
                      {formatPrice(alert.targetPriceUzs)} {t('common.currency')}
                    </span>
                  </div>
                </div>
              </div>

              <button
                onClick={() => handleDelete(alert.id)}
                className="p-2 text-gray-400 hover:text-rose-600 dark:hover:text-rose-400 rounded-xl transition-colors"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
