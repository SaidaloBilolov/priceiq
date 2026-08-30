import React, { useState, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { 
  Plus, Edit, Trash2, Search, Upload, Link as LinkIcon, 
  X, Check, ShoppingBag, RefreshCw 
} from 'lucide-react';
import { Product, Category } from '../types';
import { api } from '../services/api';

export const AdminPage: React.FC = () => {
  const { t } = useTranslation();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');

  // Admin Auth State
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => {
    return localStorage.getItem('priceiq_admin_auth') === 'true';
  });
  const [loginUsername, setLoginUsername] = useState('admin');
  const [loginPassword, setLoginPassword] = useState('admin123');
  const [loginError, setLoginError] = useState('');

  const handleAdminLogin = (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError('');
    if (loginUsername.trim() === 'admin' && loginPassword.trim() === 'admin123') {
      localStorage.setItem('priceiq_admin_auth', 'true');
      setIsAuthenticated(true);
    } else {
      setLoginError('Login yoki parol noto\'g\'ri (Standart: admin / admin123)');
    }
  };

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [imageMode, setImageMode] = useState<'url' | 'upload'>('url');
  const [uploadingImage, setUploadingImage] = useState(false);
  const [formError, setFormError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Form Fields
  const [titleUz, setTitleUz] = useState('');
  const [brand, setBrand] = useState('Apple');
  const [storage, setStorage] = useState('');
  const [ram, setRam] = useState('');
  const [color, setColor] = useState('');
  const [descriptionUz, setDescriptionUz] = useState('');
  const [priceUzs, setPriceUzs] = useState('1000000');
  const [storeName, setStoreName] = useState('Uzum Market');
  const [storeOfferUrl, setStoreOfferUrl] = useState('https://uzum.uz');
  const [imageUrl, setImageUrl] = useState('');
  const [categoryId, setCategoryId] = useState<number | undefined>(undefined);

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const [prodsData, catsData] = await Promise.all([
        api.getProducts(),
        api.getCategories()
      ]);
      setProducts(prodsData);
      setCategories(catsData);
      if (catsData.length > 0 && !categoryId) {
        setCategoryId(catsData[0].id);
      }
    } catch (err: any) {
      console.error(err);
      setFormError(err?.message || 'Serverga ulanishda xatolik yuz berdi');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, []);

  const openAddModal = () => {
    setEditingProduct(null);
    setTitleUz('');
    setBrand('Apple');
    setStorage('');
    setRam('');
    setColor('');
    setDescriptionUz('');
    setPriceUzs('1000000');
    setStoreName('Uzum Market');
    setStoreOfferUrl('https://uzum.uz');
    setImageUrl('');
    setImageMode('url');
    setFormError('');
    setIsModalOpen(true);
  };

  const openEditModal = (product: Product) => {
    setEditingProduct(product);
    setTitleUz(product.titleUz || '');
    setBrand(product.brand || 'General');
    setStorage(product.storage || '');
    setRam(product.ram || '');
    setColor(product.color || '');
    setDescriptionUz(product.descriptionUz || '');
    setPriceUzs(product.lowestPriceUzs ? product.lowestPriceUzs.toString() : '1000000');
    setStoreName(product.storeName || (product.offers && product.offers[0]?.store?.name) || 'Uzum Market');
    setStoreOfferUrl(product.storeOfferUrl || (product.offers && product.offers[0]?.offerUrl) || 'https://uzum.uz');
    setImageUrl(product.imageUrl || '');
    setCategoryId(product.categoryId || (product.category ? product.category.id : undefined));
    setImageMode('url');
    setFormError('');
    setIsModalOpen(true);
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    const file = files[0];
    setUploadingImage(true);
    setFormError('');

    try {
      const res = await api.uploadProductImage(file);
      let fullUrl = res.imageUrl;
      if (fullUrl.startsWith('/')) {
        const baseUrl = (import.meta as any).env?.VITE_API_BASE_URL || 'http://localhost:5001/api';
        const serverOrigin = baseUrl.replace('/api', '');
        fullUrl = `${serverOrigin}${fullUrl}`;
      }
      setImageUrl(fullUrl);
    } catch (err: any) {
      console.error(err);
      setFormError('Kompyuterdan rasm yuklashda xatolik: ' + (err.message || 'Server unreachable'));
    } finally {
      setUploadingImage(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!titleUz.trim()) {
      setFormError('Mahsulot nomini kiriting');
      return;
    }
    if (!priceUzs || Number(priceUzs) <= 0) {
      setFormError('To\'g\'ri narx kiriting');
      return;
    }

    const payload: Partial<Product> = {
      titleUz: titleUz.trim(),
      titleRu: titleUz.trim(),
      titleEn: titleUz.trim(),
      brand: brand.trim() || 'General',
      storage: storage.trim(),
      ram: ram.trim(),
      color: color.trim(),
      descriptionUz: descriptionUz.trim(),
      descriptionRu: descriptionUz.trim(),
      descriptionEn: descriptionUz.trim(),
      priceUzs: Number(priceUzs),
      storeName: storeName.trim() || 'Uzum Market',
      storeOfferUrl: storeOfferUrl.trim() || 'https://uzum.uz',
      imageUrl: imageUrl.trim() || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80',
      categoryId
    };

    try {
      if (editingProduct) {
        await api.updateProduct(editingProduct.id, payload);
        setSuccessMsg('Mahsulot muvaffaqiyatli yangilandi!');
      } else {
        await api.createProduct(payload);
        setSuccessMsg('Yangi mahsulot saqlandi!');
      }

      setIsModalOpen(false);
      fetchProducts();
      setTimeout(() => setSuccessMsg(''), 3500);
    } catch (err: any) {
      console.error(err);
      setFormError('Xatolik: ' + (err.message || 'Mahsulotni saqlash imkonsiz bo\'ldi'));
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Haqiqatdan ham ushbu mahsulotni o\'chirmoqchimisiz?')) return;
    try {
      await api.deleteProduct(id);
      setSuccessMsg('Mahsulot o\'chirildi!');
      fetchProducts();
      setTimeout(() => setSuccessMsg(''), 3500);
    } catch (err: any) {
      console.error(err);
      setFormError('O\'chirishda xatolik: ' + (err.message || 'Failed to delete'));
    }
  };

  const formatPrice = (amount: number) => {
    return new Intl.NumberFormat('fr-FR').format(amount).replace(/,/g, ' ');
  };

  const filteredProducts = products.filter((p) =>
    (p.titleUz || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
    (p.brand || '').toLowerCase().includes(searchQuery.toLowerCase())
  );

  if (!isAuthenticated) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center p-4">
        <div className="bg-white dark:bg-gray-800 p-8 rounded-3xl border border-gray-100 dark:border-gray-700 shadow-2xl max-w-md w-full space-y-6">
          <div className="text-center space-y-2">
            <div className="w-12 h-12 bg-blue-600 text-white font-black text-xl rounded-2xl flex items-center justify-center mx-auto shadow-lg shadow-blue-500/25">
              IQ
            </div>
            <h2 className="text-xl font-black text-gray-900 dark:text-white">Admin Tizimiga Kirish</h2>
            <p className="text-xs text-gray-500">Boshqaruv paneliga kirish uchun parolni kiriting</p>
          </div>

          {loginError && (
            <div className="p-3 bg-rose-50 text-rose-600 dark:bg-rose-950/60 dark:text-rose-300 text-xs font-bold rounded-2xl">
              {loginError}
            </div>
          )}

          <form onSubmit={handleAdminLogin} className="space-y-4 text-xs font-bold">
            <div>
              <label className="block text-gray-700 dark:text-gray-300 mb-1">Username</label>
              <input
                type="text"
                required
                value={loginUsername}
                onChange={(e) => setLoginUsername(e.target.value)}
                placeholder="admin"
                className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-gray-700 dark:text-gray-300 mb-1">Password</label>
              <input
                type="password"
                required
                value={loginPassword}
                onChange={(e) => setLoginPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div className="p-2.5 bg-gray-50 dark:bg-gray-900/80 rounded-xl text-[11px] font-mono text-gray-500">
              Standart Login: <b>admin</b> | Parol: <b>admin123</b>
            </div>
            <button
              type="submit"
              className="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white rounded-2xl font-black text-xs shadow-lg shadow-blue-500/25 transition-all"
            >
              Tizimga Kirish
            </button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6 pb-24 max-w-6xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white dark:bg-gray-800 p-6 rounded-3xl border border-gray-100 dark:border-gray-700/60 shadow-lg">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-blue-50 dark:bg-blue-900/40 text-blue-600 dark:text-blue-400 rounded-lg text-xs font-black uppercase tracking-wider mb-2">
            <ShoppingBag className="w-3.5 h-3.5" />
            <span>PRICEIQ Universal Admin</span>
          </div>
          <h1 className="text-2xl font-black text-gray-900 dark:text-white tracking-tight">
            Universal Mahsulotlar Boshqaruvi
          </h1>
          <p className="text-xs text-gray-500 mt-1">
            Barcha turdagi mahsulotlarni (Smartfon, Noutbuk, Kiyim, Maishiy texnika) qo'shish va narxlarni boshqarish
          </p>
        </div>

        <button
          onClick={openAddModal}
          className="px-5 py-3 bg-blue-600 hover:bg-blue-700 text-white text-xs font-black rounded-2xl flex items-center justify-center gap-2 shadow-lg shadow-blue-500/25 transition-all active:scale-95"
        >
          <Plus className="w-4 h-4" />
          <span>Yangi Mahsulot Qo'shish</span>
        </button>
      </div>

      {/* Success Notification */}
      {successMsg && (
        <div className="p-4 bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300 font-extrabold text-xs rounded-2xl flex items-center gap-2 border border-emerald-200 dark:border-emerald-800 animate-fadeIn">
          <Check className="w-4 h-4" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* Controls Bar */}
      <div className="flex items-center justify-between gap-3">
        <div className="relative flex-1 max-w-md">
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Mahsulot nomi yoki brend bo'yicha qidiruv..."
            className="w-full pl-10 pr-4 py-2.5 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-2xl text-xs font-bold text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 shadow-sm"
          />
          <Search className="w-4 h-4 text-gray-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
        </div>

        <button
          onClick={fetchProducts}
          className="p-2.5 bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-300 hover:text-blue-600 rounded-2xl border border-gray-200 dark:border-gray-700 transition-colors"
          title="Yangilash"
        >
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>

      {/* Products Data Table */}
      <div className="bg-white dark:bg-gray-800 rounded-3xl border border-gray-100 dark:border-gray-700/60 shadow-lg overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-16">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
          </div>
        ) : filteredProducts.length === 0 ? (
          <div className="text-center py-16">
            <ShoppingBag className="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <p className="text-sm font-bold text-gray-500">Mahsulotlar topilmadi</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 dark:bg-gray-900/60 border-b border-gray-100 dark:border-gray-700 text-[11px] font-black uppercase text-gray-500 tracking-wider">
                  <th className="py-3.5 px-4">Rasm & Nomi</th>
                  <th className="py-3.5 px-4">Brend & Kategoriya</th>
                  <th className="py-3.5 px-4">Eng Arzon Narx</th>
                  <th className="py-3.5 px-4">Do'kon</th>
                  <th className="py-3.5 px-4 text-right">Amallar</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 dark:divide-gray-700/60 text-xs">
                {filteredProducts.map((p) => (
                  <tr key={p.id} className="hover:bg-gray-50/80 dark:hover:bg-gray-700/30 transition-colors">
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-3">
                        <img
                          src={p.imageUrl}
                          alt={p.titleUz}
                          className="w-10 h-10 rounded-xl object-contain bg-gray-50 dark:bg-gray-900 p-1 border border-gray-200 dark:border-gray-700 shrink-0"
                        />
                        <div>
                          <span className="font-extrabold text-gray-900 dark:text-white line-clamp-1">
                            {p.titleUz}
                          </span>
                          {p.descriptionUz && (
                            <span className="text-[10px] text-gray-400 line-clamp-1">
                              {p.descriptionUz}
                            </span>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="py-3 px-4">
                      <span className="px-2 py-0.5 bg-blue-50 text-blue-600 dark:bg-blue-950 dark:text-blue-300 font-extrabold rounded-md text-[10px] uppercase">
                        {p.brand || 'General'}
                      </span>
                      <span className="text-[11px] font-medium text-gray-500 block mt-0.5">
                        {p.category?.nameUz || 'Kategoriya'} {p.storage ? `• ${p.storage}` : ''}
                      </span>
                    </td>
                    <td className="py-3 px-4 font-black text-blue-600 dark:text-blue-400">
                      {formatPrice(p.lowestPriceUzs || p.priceUzs || 0)} <span className="text-[10px] font-semibold">{t('common.currency')}</span>
                    </td>
                    <td className="py-3 px-4 text-gray-600 dark:text-gray-300 font-bold">
                      {p.storeName || (p.offers && p.offers[0]?.store?.name) || 'Uzum Market'}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => openEditModal(p)}
                          className="p-2 text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-950/60 rounded-xl transition-colors"
                          title="Tahrirlash"
                        >
                          <Edit className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleDelete(p.id)}
                          className="p-2 text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/60 rounded-xl transition-colors"
                          title="O'chirish"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Add / Edit Product Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fadeIn">
          <div className="bg-white dark:bg-gray-800 rounded-3xl max-w-lg w-full p-6 shadow-2xl relative border border-gray-100 dark:border-gray-700 max-h-[90vh] overflow-y-auto">
            <button
              onClick={() => setIsModalOpen(false)}
              className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 dark:hover:text-white p-1 rounded-lg"
            >
              <X className="w-5 h-5" />
            </button>

            <h2 className="text-lg font-black text-gray-900 dark:text-white mb-4 flex items-center gap-2">
              <ShoppingBag className="w-5 h-5 text-blue-600" />
              <span>{editingProduct ? 'Mahsulotni Tahrirlash' : 'Yangi Mahsulot Qo\'shish'}</span>
            </h2>

            {formError && (
              <div className="p-3 mb-4 bg-rose-50 text-rose-600 dark:bg-rose-950/60 text-xs font-bold rounded-2xl">
                {formError}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4 text-xs font-bold">
              <div>
                <label className="block text-gray-700 dark:text-gray-300 mb-1">
                  Mahsulot Nomi (Title / Name) *
                </label>
                <input
                  type="text"
                  required
                  value={titleUz}
                  onChange={(e) => setTitleUz(e.target.value)}
                  placeholder="masalan: LG TV 55 / Nike Air Max / Asus ROG Noutbuk"
                  className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-blue-500 font-bold"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-gray-700 dark:text-gray-300 mb-1">Kategoriya</label>
                  <select
                    value={categoryId || ''}
                    onChange={(e) => setCategoryId(Number(e.target.value))}
                    className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-blue-500 font-bold"
                  >
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.nameUz}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-gray-700 dark:text-gray-300 mb-1">Brend (Ixtiyoriy)</label>
                  <input
                    type="text"
                    value={brand}
                    onChange={(e) => setBrand(e.target.value)}
                    placeholder="Apple, Samsung, Nike, LG..."
                    className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-blue-500 font-bold"
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-gray-700 dark:text-gray-300 mb-1">Xotira (ixtiyoriy)</label>
                  <input
                    type="text"
                    value={storage}
                    onChange={(e) => setStorage(e.target.value)}
                    placeholder="256GB / 1TB"
                    className="w-full px-3 py-2 bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl text-gray-900 dark:text-white outline-none"
                  />
                </div>
                <div>
                  <label className="block text-gray-700 dark:text-gray-300 mb-1">RAM (ixtiyoriy)</label>
                  <input
                    type="text"
                    value={ram}
                    onChange={(e) => setRam(e.target.value)}
                    placeholder="16GB"
                    className="w-full px-3 py-2 bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl text-gray-900 dark:text-white outline-none"
                  />
                </div>
                <div>
                  <label className="block text-gray-700 dark:text-gray-300 mb-1">Rangi (ixtiyoriy)</label>
                  <input
                    type="text"
                    value={color}
                    onChange={(e) => setColor(e.target.value)}
                    placeholder="Qora"
                    className="w-full px-3 py-2 bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl text-gray-900 dark:text-white outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-gray-700 dark:text-gray-300 mb-1">
                  Tavsifi / Haqida (Description / About)
                </label>
                <textarea
                  rows={2}
                  value={descriptionUz}
                  onChange={(e) => setDescriptionUz(e.target.value)}
                  placeholder="Mahsulot haqida ma'lumot va parametri..."
                  className="w-full px-3.5 py-2 bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl text-gray-900 dark:text-white outline-none focus:ring-2 focus:ring-blue-500 font-medium"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-gray-700 dark:text-gray-300 mb-1">Narxi (So'mda) *</label>
                  <input
                    type="number"
                    required
                    value={priceUzs}
                    onChange={(e) => setPriceUzs(e.target.value)}
                    placeholder="12500000"
                    className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl text-gray-900 dark:text-white outline-none font-black"
                  />
                </div>
                <div>
                  <label className="block text-gray-700 dark:text-gray-300 mb-1">Do'kon Nomi</label>
                  <input
                    type="text"
                    value={storeName}
                    onChange={(e) => setStoreName(e.target.value)}
                    placeholder="Uzum Market / Texnomart"
                    className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl text-gray-900 dark:text-white outline-none font-bold"
                  />
                </div>
              </div>

              <div>
                <label className="block text-gray-700 dark:text-gray-300 mb-1">Mahsulot Havolasi (Offer Link)</label>
                <input
                  type="url"
                  value={storeOfferUrl}
                  onChange={(e) => setStoreOfferUrl(e.target.value)}
                  placeholder="https://uzum.uz/product/..."
                  className="w-full px-3.5 py-2 bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl text-gray-900 dark:text-white outline-none font-medium"
                />
              </div>

              {/* Image Input Tab Toggle */}
              <div>
                <label className="block text-gray-700 dark:text-gray-300 mb-1.5">
                  Mahsulot Rasmi (Image Input)
                </label>
                <div className="flex items-center gap-2 mb-2 bg-gray-100 dark:bg-gray-900 p-1 rounded-xl">
                  <button
                    type="button"
                    onClick={() => setImageMode('url')}
                    className={`flex-1 py-1.5 rounded-lg text-center font-extrabold transition-all flex items-center justify-center gap-1 ${
                      imageMode === 'url'
                        ? 'bg-blue-600 text-white shadow-sm'
                        : 'text-gray-600 dark:text-gray-400'
                    }`}
                  >
                    <LinkIcon className="w-3.5 h-3.5" />
                    <span>Ssilka Orqali</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setImageMode('upload')}
                    className={`flex-1 py-1.5 rounded-lg text-center font-extrabold transition-all flex items-center justify-center gap-1 ${
                      imageMode === 'upload'
                        ? 'bg-blue-600 text-white shadow-sm'
                        : 'text-gray-600 dark:text-gray-400'
                    }`}
                  >
                    <Upload className="w-3.5 h-3.5" />
                    <span>Kompyuterdan Rasm Yuklash</span>
                  </button>
                </div>

                {imageMode === 'url' ? (
                  <input
                    type="url"
                    value={imageUrl}
                    onChange={(e) => setImageUrl(e.target.value)}
                    placeholder="https://images.unsplash.com/photo-..."
                    className="w-full px-3.5 py-2.5 bg-gray-50 dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-xl text-gray-900 dark:text-white outline-none"
                  />
                ) : (
                  <div>
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept="image/*"
                      onChange={handleFileUpload}
                      className="hidden"
                    />
                    <button
                      type="button"
                      onClick={() => fileInputRef.current?.click()}
                      disabled={uploadingImage}
                      className="w-full py-3 border-2 border-dashed border-gray-300 dark:border-gray-700 hover:border-blue-500 rounded-2xl flex items-center justify-center gap-2 text-gray-600 dark:text-gray-300 font-bold bg-gray-50 dark:bg-gray-900/60 transition-colors"
                    >
                      <Upload className="w-4 h-4 text-blue-600" />
                      <span>{uploadingImage ? 'Rasm yuklanmoqda...' : 'Kompyuterdan rasm tanlang (.jpg, .png)'}</span>
                    </button>
                  </div>
                )}

                {imageUrl && (
                  <div className="mt-2 flex items-center gap-3 bg-gray-50 dark:bg-gray-900 p-2 rounded-2xl border border-gray-200 dark:border-gray-700">
                    <img src={imageUrl} alt="Preview" className="w-12 h-12 object-contain rounded-lg bg-white p-1" />
                    <span className="text-[10px] text-gray-500 line-clamp-1 flex-1 font-mono">{imageUrl}</span>
                  </div>
                )}
              </div>

              <div className="flex items-center gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="flex-1 py-3 bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 text-gray-700 dark:text-gray-300 rounded-2xl font-bold"
                >
                  Bekor Qilish
                </button>
                <button
                  type="submit"
                  className="flex-1 py-3 bg-blue-600 hover:bg-blue-700 text-white rounded-2xl font-black shadow-lg shadow-blue-500/25"
                >
                  {editingProduct ? 'Saqlash' : 'Qo\'shish'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
