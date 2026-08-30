import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Smartphone, Store, Bell, TrendingUp, Plus, ArrowRight, ShieldCheck } from 'lucide-react';
import { Product, Category } from '../types';
import { api } from '../services/api';

export const Dashboard: React.FC = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [prodsData, catsData] = await Promise.all([
          api.getProducts(),
          api.getCategories()
        ]);
        setProducts(prodsData);
        setCategories(catsData);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const totalOffers = products.reduce((acc, p) => acc + (p.offers?.length || 0), 0);
  const avgPrice = products.length > 0
    ? Math.round(products.reduce((acc, p) => acc + (p.lowestPriceUzs || 0), 0) / products.length)
    : 0;

  const formatPrice = (amount: number) => {
    return new Intl.NumberFormat('fr-FR').format(amount).replace(/,/g, ' ');
  };

  return (
    <div className="space-y-6 pb-20">
      {/* Welcome Banner */}
      <div className="bg-gradient-to-r from-blue-900 via-indigo-900 to-slate-900 p-8 rounded-3xl border border-blue-800/40 shadow-xl relative overflow-hidden">
        <div className="relative z-10 space-y-3 max-w-2xl">
          <span className="px-3 py-1 bg-blue-500/20 text-blue-300 border border-blue-400/30 rounded-full text-xs font-black uppercase tracking-wider inline-flex items-center gap-1.5">
            <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
            <span>PRICEIQ Master Console</span>
          </span>
          <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight">
            Xush Kelibsiz, Admin!
          </h1>
          <p className="text-xs sm:text-sm text-slate-300 leading-relaxed font-medium">
            Smartfonlar narxlarini monitoring qilish, yangi modellar qo'shish va do'kon takliflarini real-vaqt rejimida boshqaring.
          </p>
          <div className="pt-2 flex items-center gap-3">
            <Link
              to="/products"
              className="px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white rounded-2xl text-xs font-black flex items-center gap-2 shadow-lg shadow-blue-500/25 transition-all"
            >
              <span>Mahsulotlar CRUD</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        </div>
      </div>

      {/* Analytics Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-slate-900 p-5 rounded-3xl border border-slate-800 space-y-3">
          <div className="p-3 bg-blue-500/10 text-blue-400 rounded-2xl w-fit">
            <Smartphone className="w-6 h-6" />
          </div>
          <div>
            <span className="text-2xl font-black text-white">{products.length}</span>
            <span className="text-xs text-slate-400 block font-bold mt-0.5">Jami Smartfonlar</span>
          </div>
        </div>

        <div className="bg-slate-900 p-5 rounded-3xl border border-slate-800 space-y-3">
          <div className="p-3 bg-emerald-500/10 text-emerald-400 rounded-2xl w-fit">
            <Store className="w-6 h-6" />
          </div>
          <div>
            <span className="text-2xl font-black text-white">{totalOffers}</span>
            <span className="text-xs text-slate-400 block font-bold mt-0.5">Do'kon Takliflari</span>
          </div>
        </div>

        <div className="bg-slate-900 p-5 rounded-3xl border border-slate-800 space-y-3">
          <div className="p-3 bg-purple-500/10 text-purple-400 rounded-2xl w-fit">
            <TrendingUp className="w-6 h-6" />
          </div>
          <div>
            <span className="text-2xl font-black text-white">{formatPrice(avgPrice)} so'm</span>
            <span className="text-xs text-slate-400 block font-bold mt-0.5">O'rtacha Narx</span>
          </div>
        </div>

        <div className="bg-slate-900 p-5 rounded-3xl border border-slate-800 space-y-3">
          <div className="p-3 bg-amber-500/10 text-amber-400 rounded-2xl w-fit">
            <Bell className="w-6 h-6" />
          </div>
          <div>
            <span className="text-2xl font-black text-white">{categories.length}</span>
            <span className="text-xs text-slate-400 block font-bold mt-0.5">Kategoriyalar</span>
          </div>
        </div>
      </div>

      {/* Recent Products Table */}
      <div className="bg-slate-900 rounded-3xl border border-slate-800 p-6 space-y-4 shadow-xl">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-black text-white uppercase tracking-wider">
            Oxirgi Qo'shilgan Smartfonlar
          </h2>
          <Link to="/products" className="text-xs font-bold text-blue-400 hover:underline">
            Barchasini ko'rish &rarr;
          </Link>
        </div>

        {loading ? (
          <div className="flex justify-center py-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
          </div>
        ) : (
          <div className="divide-y divide-slate-800">
            {products.slice(0, 5).map((p) => (
              <div key={p.id} className="py-3 flex items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <img
                    src={p.imageUrl}
                    alt={p.titleUz}
                    className="w-10 h-10 rounded-xl object-contain bg-slate-950 p-1 border border-slate-800"
                  />
                  <div>
                    <h3 className="text-xs font-extrabold text-white line-clamp-1">{p.titleUz}</h3>
                    <span className="text-[10px] text-slate-400 font-medium">
                      {p.brand} • {p.storage} • {p.color}
                    </span>
                  </div>
                </div>
                <div className="text-right">
                  <span className="text-xs font-black text-blue-400 block">
                    {formatPrice(p.lowestPriceUzs)} so'm
                  </span>
                  <span className="text-[10px] text-emerald-400 font-bold uppercase">
                    {p.dealBadgeUz}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
