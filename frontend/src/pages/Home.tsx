import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Search, Sparkles, Smartphone, ShieldCheck, Tag } from 'lucide-react';
import { Product, Category, Language } from '../types';
import { api } from '../services/api';
import { getTelegramUser } from '../utils/telegram';
import { ProductCard } from '../components/ProductCard';

export const Home: React.FC = () => {
  const { i18n, t } = useTranslation();
  const navigate = useNavigate();
  const tgUser = getTelegramUser();
  const currentLang = (i18n.language || 'uz') as Language;

  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [favorites, setFavorites] = useState<number[]>([]);
  const [searchVal, setSearchVal] = useState('');
  const [selectedCat, setSelectedCat] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [prodsData, catsData, favsData] = await Promise.all([
          api.getProducts(),
          api.getCategories(),
          api.getFavorites(tgUser.id)
        ]);
        setProducts(prodsData);
        setCategories(catsData);
        setFavorites(favsData.map((f) => f.id));
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchVal.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchVal)}`);
    }
  };

  const handleToggleFavorite = async (productId: number) => {
    try {
      const res = await api.toggleFavorite(tgUser.id, productId);
      if (res.favorited) {
        setFavorites((prev) => [...prev, productId]);
      } else {
        setFavorites((prev) => prev.filter((id) => id !== productId));
      }
    } catch (err) {
      console.error(err);
    }
  };

  const getCatName = (cat: Category) => {
    if (currentLang === 'ru') return cat.nameRu;
    if (currentLang === 'en') return cat.nameEn;
    return cat.nameUz;
  };

  return (
    <div className="space-y-6 pb-20">
      {/* Hero Banner */}
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-blue-700 via-blue-600 to-indigo-800 text-white p-6 md:p-8 shadow-xl">
        <div className="absolute -right-10 -bottom-10 w-72 h-72 bg-white/10 rounded-full blur-3xl pointer-events-none" />
        
        <div className="relative z-10 space-y-4">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/15 backdrop-blur-md text-[10px] font-black uppercase tracking-wider text-blue-100 border border-white/20">
            <Sparkles className="w-3 h-3 text-yellow-300" />
            <span>@princeiquz_bot Smart Assistant</span>
          </div>

          <h1 className="text-2xl md:text-3xl font-black tracking-tight leading-tight">
            {t('home.title')}
          </h1>

          <p className="text-blue-100 text-xs md:text-sm leading-relaxed">
            {t('home.subtitle')}
          </p>

          <form onSubmit={handleSearchSubmit} className="relative pt-1">
            <input
              type="text"
              value={searchVal}
              onChange={(e) => setSearchVal(e.target.value)}
              placeholder={t('home.searchPlaceholder')}
              className="w-full pl-10 pr-24 py-3 bg-white dark:bg-gray-900 rounded-2xl text-gray-900 dark:text-white placeholder-gray-400 text-xs font-bold shadow-lg focus:outline-none focus:ring-4 focus:ring-blue-300 transition-all"
            />
            <Search className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            <button
              type="submit"
              className="absolute right-1.5 top-1.5 bottom-1.5 px-4 bg-blue-600 hover:bg-blue-700 text-white font-black text-[11px] rounded-xl transition-colors"
            >
              {t('common.search')}
            </button>
          </form>
        </div>
      </section>

      {/* Brand & Category Carousel */}
      <section>
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-sm font-black text-gray-900 dark:text-white uppercase tracking-wider">
            {t('home.categories')}
          </h2>
        </div>

        <div className="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-none">
          <button
            onClick={() => {
              setSelectedCat(null);
              api.getProducts().then(setProducts);
            }}
            className={`px-4 py-2 rounded-2xl text-xs font-extrabold whitespace-nowrap transition-all ${
              selectedCat === null
                ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20'
                : 'bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 border border-gray-100 dark:border-gray-700'
            }`}
          >
            {t('common.all')}
          </button>
          {categories.map((cat) => (
            <button
              key={cat.id}
              onClick={() => {
                setSelectedCat(cat.id);
                api.getProducts({ categoryId: cat.id }).then(setProducts);
              }}
              className={`px-4 py-2 rounded-2xl text-xs font-extrabold whitespace-nowrap flex items-center gap-1.5 transition-all ${
                selectedCat === cat.id
                  ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20'
                  : 'bg-white dark:bg-gray-800 text-gray-700 dark:text-gray-300 border border-gray-100 dark:border-gray-700'
              }`}
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span>{getCatName(cat)}</span>
            </button>
          ))}
        </div>
      </section>

      {/* Best Deals Grid */}
      <section>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-black text-gray-900 dark:text-white uppercase tracking-wider">
            {t('home.bestDeals')}
          </h2>
        </div>

        {loading ? (
          <div className="flex items-center justify-center min-h-[200px]">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
            {products.map((product) => (
              <ProductCard
                key={product.id}
                product={product}
                isFavorite={favorites.includes(product.id)}
                onToggleFavorite={handleToggleFavorite}
              />
            ))}
          </div>
        )}
      </section>
    </div>
  );
};
