import React from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { Heart, Store, ArrowRight } from 'lucide-react';
import { Product, Language } from '../types';

interface ProductCardProps {
  product: Product;
  isFavorite?: boolean;
  onToggleFavorite?: (productId: number) => void;
}

export const ProductCard: React.FC<ProductCardProps> = ({ product, isFavorite, onToggleFavorite }) => {
  const { i18n, t } = useTranslation();
  const currentLang = (i18n.language || 'uz') as Language;

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

  return (
    <div className="group relative bg-white dark:bg-gray-800 rounded-3xl border border-gray-100 dark:border-gray-700/60 shadow-sm hover:shadow-xl transition-all duration-300 flex flex-col overflow-hidden">
      {/* Deal Score Badge */}
      <div className="absolute top-3 left-3 z-10">
        <span className="px-2.5 py-1 text-[10px] font-black rounded-lg bg-emerald-500 text-white shadow-md uppercase tracking-wider">
          {getDealBadge()}
        </span>
      </div>

      {/* Favorite Button */}
      {onToggleFavorite && (
        <button
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
            onToggleFavorite(product.id);
          }}
          className={`absolute top-3 right-3 z-10 p-2 rounded-full transition-all backdrop-blur-md ${
            isFavorite
              ? 'bg-rose-50 text-rose-600 dark:bg-rose-950/60 dark:text-rose-400'
              : 'bg-white/80 text-gray-400 hover:text-rose-500 dark:bg-gray-900/60'
          }`}
        >
          <Heart className={`w-4 h-4 ${isFavorite ? 'fill-current' : ''}`} />
        </button>
      )}

      {/* Image */}
      <Link to={`/product/${product.id}`} className="block relative aspect-square bg-gray-50 dark:bg-gray-900 p-4">
        <img
          src={product.imageUrl}
          alt={getTitle()}
          className="w-full h-full object-contain group-hover:scale-105 transition-transform duration-300"
          loading="lazy"
        />
      </Link>

      {/* Details */}
      <div className="p-4 flex-1 flex flex-col justify-between space-y-3">
        <div>
          <span className="text-[10px] font-extrabold text-blue-600 dark:text-blue-400 uppercase tracking-widest block mb-1">
            {product.brand}
          </span>
          <Link to={`/product/${product.id}`}>
            <h3 className="text-xs font-bold text-gray-900 dark:text-white line-clamp-2 hover:text-blue-600 transition-colors leading-snug">
              {getTitle()}
            </h3>
          </Link>
        </div>

        <div className="pt-2 border-t border-gray-100 dark:border-gray-700/60">
          <div className="flex items-center text-[10px] text-gray-400 gap-1 mb-1 font-semibold">
            <Store className="w-3 h-3 text-gray-400" />
            <span>{product.offers?.length || 0} {t('home.storesCompared')}</span>
          </div>

          <div className="flex items-baseline justify-between">
            <div>
              <span className="text-[10px] text-gray-400 block font-medium">
                {t('product.lowestPrice')}
              </span>
              <span className="text-base font-black text-blue-600 dark:text-blue-400">
                {formatPrice(product.lowestPriceUzs)} <span className="text-xs font-semibold">{t('common.currency')}</span>
              </span>
            </div>

            <Link
              to={`/product/${product.id}`}
              className="p-1.5 bg-blue-50 dark:bg-blue-900/40 text-blue-600 dark:text-blue-400 rounded-xl hover:bg-blue-600 hover:text-white transition-all flex items-center gap-1 text-[10px] font-extrabold"
            >
              <span>{t('home.viewDetails')}</span>
              <ArrowRight className="w-3 h-3" />
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
