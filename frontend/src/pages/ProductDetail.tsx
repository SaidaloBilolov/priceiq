import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Heart, Bell, ExternalLink, ArrowLeft, CheckCircle, Store as StoreIcon, TrendingDown, Shield } from 'lucide-react';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer } from 'recharts';
import { Product, Language } from '../types';
import { api } from '../services/api';
import { getTelegramUser } from '../utils/telegram';
import { PriceAlertModal } from '../components/PriceAlertModal';

export const ProductDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { i18n, t } = useTranslation();
  const tgUser = getTelegramUser();
  const currentLang = (i18n.language || 'uz') as Language;

  const [product, setProduct] = useState<Product | null>(null);
  const [isFavorite, setIsFavorite] = useState(false);
  const [loading, setLoading] = useState(true);
  const [showAlertModal, setShowAlertModal] = useState(false);

  useEffect(() => {
    if (id) {
      setLoading(true);
      Promise.all([
        api.getProductById(id),
        api.getFavorites(tgUser.id)
      ])
        .then(([prodData, favsData]) => {
          setProduct(prodData);
          setIsFavorite(favsData.some((f) => f.id === prodData.id));
          setLoading(false);
        })
        .catch((err) => {
          console.error(err);
          setLoading(false);
        });
    }
  }, [id]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!product) {
    return (
      <div className="text-center py-16">
        <h2 className="text-sm font-bold text-gray-900 dark:text-white mb-4">
          {t('common.error')}
        </h2>
        <Link to="/" className="text-xs text-blue-600 font-bold hover:underline">
          {t('common.back')} {t('navigation.home')}
        </Link>
      </div>
    );
  }

  const getTitle = () => {
    if (currentLang === 'ru') return product.titleRu || product.titleUz;
    if (currentLang === 'en') return product.titleEn || product.titleUz;
    return product.titleUz;
  };

  const getDealBadge = () => {
    if (currentLang === 'ru') return product.dealBadgeRu;
    if (currentLang === 'en') return product.dealBadgeEn;
    return product.dealBadgeUz;
  };

  const formatPrice = (amount: number) => {
    return new Intl.NumberFormat('fr-FR').format(amount).replace(/,/g, ' ');
  };

  const formatDate = (dateStr: string) => {
    try {
      const d = new Date(dateStr);
      return `${d.getDate()}/${d.getMonth() + 1}`;
    } catch {
      return dateStr;
    }
  };

  const handleToggleFav = async () => {
    try {
      const res = await api.toggleFavorite(tgUser.id, product.id);
      setIsFavorite(res.favorited);
    } catch (err) {
      console.error(err);
    }
  };

  const chartData = product.priceHistory?.map((ph) => ({
    date: formatDate(ph.recordedAt),
    price: ph.priceUzs
  })) || [];

  return (
    <div className="space-y-6 pb-24">
      {/* Top Bar */}
      <Link
        to="/"
        className="inline-flex items-center gap-1 text-xs font-extrabold text-gray-500 hover:text-blue-600 transition-colors"
      >
        <ArrowLeft className="w-4 h-4" />
        <span>{t('common.back')}</span>
      </Link>

      {/* Main Image & Stats */}
      <div className="bg-white dark:bg-gray-800 rounded-3xl p-6 border border-gray-100 dark:border-gray-700/60 shadow-lg space-y-6">
        <div className="relative aspect-square bg-gray-50 dark:bg-gray-900 rounded-2xl p-4 flex items-center justify-center border border-gray-100 dark:border-gray-800">
          <span className="absolute top-3 left-3 px-3 py-1 bg-emerald-500 text-white font-black text-xs rounded-xl shadow uppercase tracking-wider">
            {getDealBadge()}
          </span>
          <img src={product.imageUrl} alt={getTitle()} className="w-full h-full object-contain" />
        </div>

        <div className="space-y-3">
          <span className="text-[10px] font-black uppercase tracking-widest text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/40 px-2.5 py-1 rounded-lg">
            {product.brand} • {product.storage} • {product.color}
          </span>
          <h1 className="text-xl font-extrabold text-gray-900 dark:text-white leading-snug">
            {getTitle()}
          </h1>

          {/* Stats Bar */}
          <div className="grid grid-cols-3 gap-2 bg-gray-50 dark:bg-gray-900 p-3 rounded-2xl border border-gray-100 dark:border-gray-800">
            <div>
              <span className="text-[10px] font-bold text-emerald-600 block">{t('product.lowestPrice')}</span>
              <span className="text-xs font-black text-gray-900 dark:text-white">
                {formatPrice(product.lowestPriceUzs)}
              </span>
            </div>
            <div>
              <span className="text-[10px] font-bold text-gray-400 block">{t('product.averagePrice')}</span>
              <span className="text-xs font-bold text-gray-700 dark:text-gray-300">
                {formatPrice(product.averagePriceUzs)}
              </span>
            </div>
            <div>
              <span className="text-[10px] font-bold text-amber-600 block">{t('product.highestPrice')}</span>
              <span className="text-xs font-bold text-gray-700 dark:text-gray-300">
                {formatPrice(product.highestPriceUzs)}
              </span>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center gap-3 pt-2">
            <button
              onClick={handleToggleFav}
              className={`flex-1 py-3 px-4 rounded-2xl text-xs font-bold flex items-center justify-center gap-2 border transition-all ${
                isFavorite
                  ? 'bg-rose-50 text-rose-600 border-rose-200 dark:bg-rose-950/60'
                  : 'bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-200 border-gray-200 dark:border-gray-700'
              }`}
            >
              <Heart className={`w-4 h-4 ${isFavorite ? 'fill-current' : ''}`} />
              <span>{isFavorite ? t('product.removeFavorite') : t('product.addFavorite')}</span>
            </button>

            <button
              onClick={() => setShowAlertModal(true)}
              className="flex-1 py-3 px-4 bg-blue-600 hover:bg-blue-700 text-white rounded-2xl text-xs font-black flex items-center justify-center gap-2 shadow-md shadow-blue-500/20"
            >
              <Bell className="w-4 h-4" />
              <span>{t('product.setAlert')}</span>
            </button>
          </div>
        </div>
      </div>

      {/* Store Prices List Sorted */}
      <section className="bg-white dark:bg-gray-800 rounded-3xl p-6 border border-gray-100 dark:border-gray-700/60 shadow-lg space-y-4">
        <h2 className="text-sm font-black text-gray-900 dark:text-white uppercase tracking-wider flex items-center gap-2">
          <StoreIcon className="w-4 h-4 text-blue-600" />
          <span>{t('product.compareStores')}</span>
        </h2>

        <div className="divide-y divide-gray-100 dark:divide-gray-700/60">
          {product.offers?.map((offer, idx) => (
            <div key={offer.id} className="py-3 flex items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <img
                  src={offer.store?.logoUrl}
                  alt={offer.store?.name}
                  className="w-8 h-8 rounded-xl object-cover border border-gray-200"
                />
                <div>
                  <div className="flex items-center gap-1.5">
                    <span className="font-extrabold text-xs text-gray-900 dark:text-white">
                      {offer.store?.name}
                    </span>
                    {idx === 0 && (
                      <span className="px-2 py-0.5 text-[9px] font-black rounded-md bg-emerald-100 text-emerald-700 dark:bg-emerald-950/80 dark:text-emerald-300">
                        {t('product.lowestBadge')}
                      </span>
                    )}
                  </div>
                  <span className="text-[10px] text-gray-400 font-medium block">
                    {offer.isAvailable ? t('product.inStock') : t('product.outOfStock')}
                  </span>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <span className="text-sm font-black text-blue-600 dark:text-blue-400">
                  {formatPrice(offer.priceUzs)} <span className="text-[10px] font-bold">{t('common.currency')}</span>
                </span>
                <a
                  href={offer.offerUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="p-2 bg-gray-900 text-white rounded-xl text-[10px] font-bold flex items-center gap-1"
                >
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Price History Chart */}
      {chartData.length > 0 && (
        <section className="bg-white dark:bg-gray-800 rounded-3xl p-6 border border-gray-100 dark:border-gray-700/60 shadow-lg space-y-4">
          <h2 className="text-sm font-black text-gray-900 dark:text-white uppercase tracking-wider flex items-center gap-2">
            <TrendingDown className="w-4 h-4 text-blue-600" />
            <span>{t('product.priceHistory')}</span>
          </h2>

          <div className="h-48 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData}>
                <XAxis dataKey="date" stroke="#888888" fontSize={10} />
                <YAxis stroke="#888888" fontSize={10} tickFormatter={(v) => `${v / 1000000}M`} />
                <Tooltip formatter={(value: any) => [`${formatPrice(Number(value))} so'm`, 'Narx']} />
                <Line type="monotone" dataKey="price" stroke="#2563eb" strokeWidth={3} dot={{ r: 4, fill: '#2563eb' }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </section>
      )}

      {showAlertModal && (
        <PriceAlertModal product={product} onClose={() => setShowAlertModal(false)} />
      )}
    </div>
  );
};
