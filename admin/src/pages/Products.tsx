import React, { useState, useEffect, useRef } from 'react';
import { 
  Plus, Edit, Trash2, Search, Upload, Link as LinkIcon, 
  X, Check, RefreshCw, ShoppingBag 
} from 'lucide-react';
import { Product, Category } from '../types';
import { api } from '../services/api';

const DEFAULT_CATEGORIES: Category[] = [
  { id: 1, nameUz: 'Smartfonlar' },
  { id: 2, nameUz: 'Noutbuklar' },
  { id: 3, nameUz: 'Maishiy Texnika' },
  { id: 4, nameUz: 'Kiyim-kechak' },
  { id: 5, nameUz: 'Aksessuarlar' },
  { id: 6, nameUz: 'Boshqa' }
];

export const Products: React.FC = () => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>(DEFAULT_CATEGORIES);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');

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
  const [customCategory, setCustomCategory] = useState('');
  const [storage, setStorage] = useState('256GB');
  const [ram, setRam] = useState('8GB');
  const [color, setColor] = useState('Qora');
  const [descriptionUz, setDescriptionUz] = useState('');
  const [priceUzs, setPriceUzs] = useState('1000000');
  const [storeName, setStoreName] = useState('Uzum Market');
  const [storeOfferUrl, setStoreOfferUrl] = useState('https://uzum.uz');
  const [imageUrl, setImageUrl] = useState('');
  const [categoryId, setCategoryId] = useState<number>(1);

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const [prodsData, catsData] = await Promise.all([
        api.getProducts(),
        api.getCategories()
      ]);
      if (prodsData && prodsData.length > 0) {
        setProducts(prodsData);
      }
      if (catsData && catsData.length > 0) {
        setCategories(catsData);
      }
    } catch (err: any) {
      console.warn('Using persistent local products', err);
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
    setCustomCategory('');
    setStorage('256GB');
    setRam('8GB');
    setColor('Qora');
    setDescriptionUz('');
    setPriceUzs('1000000');
    setStoreName('Uzum Market');
    setStoreOfferUrl('https://uzum.uz');
    setImageUrl('https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80');
    setImageMode('url');
    setCategoryId(1);
    setFormError('');
    setIsModalOpen(true);
  };

  const openEditModal = (product: Product) => {
    setEditingProduct(product);
    setTitleUz(product.titleUz || '');
    setBrand(product.brand || 'General');
    setCustomCategory('');
    setStorage(product.storage || '');
    setRam(product.ram || '');
    setColor(product.color || '');
    setDescriptionUz(product.descriptionUz || '');
    setPriceUzs(product.lowestPriceUzs ? product.lowestPriceUzs.toString() : '1000000');
    setStoreName(product.storeName || (product.offers && product.offers[0]?.store?.name) || 'Uzum Market');
    setStoreOfferUrl(product.storeOfferUrl || (product.offers && product.offers[0]?.offerUrl) || 'https://uzum.uz');
    setImageUrl(product.imageUrl || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80');
    setCategoryId(product.categoryId || (product.category ? product.category.id : 1));
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
      setImageUrl(res.imageUrl);
    } catch (err: any) {
      console.warn('File upload fallback');
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

    const priceNum = Number(priceUzs);
    const selectedCat = categories.find(c => c.id === categoryId) || DEFAULT_CATEGORIES[0];

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
      priceUzs: priceNum,
      storeName: storeName.trim() || 'Uzum Market',
      storeOfferUrl: storeOfferUrl.trim() || 'https://uzum.uz',
      imageUrl: imageUrl.trim() || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80',
      categoryId: categoryId,
      category: customCategory.trim() ? { id: 99, nameUz: customCategory.trim() } : selectedCat
    };

    if (editingProduct) {
      const updated = await api.updateProduct(editingProduct.id, payload);
      setProducts(prev => prev.map(p => p.id === editingProduct.id ? { ...p, ...updated } : p));
      setSuccessMsg('Mahsulot muvaffaqiyatli yangilandi!');
    } else {
      const created = await api.createProduct(payload);
      setProducts(prev => [created, ...prev.filter(p => p.id !== created.id)]);
      setSuccessMsg('Yangi mahsulot muvaffaqiyatli saqlandi!');
    }

    setIsModalOpen(false);
    setTimeout(() => setSuccessMsg(''), 3500);
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Haqiqatdan ham ushbu mahsulotni o\'chirmoqchimisiz?')) return;
    await api.deleteProduct(id);
    setProducts(prev => prev.filter(p => p.id !== id));
    setSuccessMsg('Mahsulot o\'chirildi!');
    setTimeout(() => setSuccessMsg(''), 3500);
  };

  const formatPrice = (amount: number) => {
    return new Intl.NumberFormat('fr-FR').format(amount).replace(/,/g, ' ');
  };

  const filteredProducts = products.filter((p) =>
    (p.titleUz || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
    (p.brand || '').toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="space-y-6 pb-20">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-900 p-6 rounded-3xl border border-slate-800 shadow-xl">
        <div>
          <h1 className="text-xl font-black text-white tracking-tight flex items-center gap-2">
            <ShoppingBag className="w-5 h-5 text-blue-500" />
            <span>Universal Mahsulotlar Boshqaruvi</span>
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Barcha turdagi mahsulotlarni qo'shish, rasmlarni yuklash va narxlarni boshqarish
          </p>
        </div>

        <button
          onClick={openAddModal}
          className="px-5 py-3 bg-blue-600 hover:bg-blue-500 text-white text-xs font-black rounded-2xl flex items-center justify-center gap-2 shadow-lg shadow-blue-500/25 transition-all active:scale-95"
        >
          <Plus className="w-4 h-4" />
          <span>Yangi Mahsulot Qo'shish</span>
        </button>
      </div>

      {/* Success Message */}
      {successMsg && (
        <div className="p-4 bg-emerald-500/10 text-emerald-400 font-extrabold text-xs rounded-2xl flex items-center gap-2 border border-emerald-500/20">
          <Check className="w-4 h-4" />
          <span>{successMsg}</span>
        </div>
      )}

      {/* Search Bar */}
      <div className="flex items-center justify-between gap-3">
        <div className="relative flex-1 max-w-md">
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Mahsulot nomi yoki brendi bo'yicha qidirish..."
            className="w-full pl-10 pr-4 py-2.5 bg-slate-900 border border-slate-800 rounded-2xl text-xs font-bold text-white outline-none focus:border-blue-500"
          />
          <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
        </div>

        <button
          onClick={fetchProducts}
          className="p-2.5 bg-slate-900 text-slate-400 hover:text-white rounded-2xl border border-slate-800 transition-colors"
        >
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>

      {/* Data Table */}
      <div className="bg-slate-900 rounded-3xl border border-slate-800 shadow-xl overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-16">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
          </div>
        ) : filteredProducts.length === 0 ? (
          <div className="text-center py-16">
            <ShoppingBag className="w-12 h-12 text-slate-600 mx-auto mb-3" />
            <p className="text-sm font-bold text-slate-400">Mahsulotlar topilmadi</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-950/60 border-b border-slate-800 text-[11px] font-black uppercase text-slate-400 tracking-wider">
                  <th className="py-3.5 px-4">Rasm & Nomi</th>
                  <th className="py-3.5 px-4">Brend & Kategoriya</th>
                  <th className="py-3.5 px-4">Eng Arzon Narx</th>
                  <th className="py-3.5 px-4">Do'kon</th>
                  <th className="py-3.5 px-4 text-right">Amallar</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800 text-xs">
                {filteredProducts.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-800/40 transition-colors">
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-3">
                        <img
                          src={p.imageUrl}
                          alt={p.titleUz}
                          className="w-10 h-10 rounded-xl object-contain bg-slate-950 p-1 border border-slate-800 shrink-0"
                        />
                        <div>
                          <span className="font-extrabold text-white line-clamp-1">{p.titleUz}</span>
                          {p.descriptionUz && (
                            <span className="text-[10px] text-slate-400 line-clamp-1">{p.descriptionUz}</span>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="py-3 px-4">
                      <span className="px-2 py-0.5 bg-blue-500/10 text-blue-400 border border-blue-500/20 font-extrabold rounded-md text-[10px] uppercase">
                        {p.brand || 'General'}
                      </span>
                      <span className="text-[11px] font-medium text-slate-400 block mt-0.5">
                        {p.category?.nameUz || 'Smartfonlar'} {p.storage ? `• ${p.storage}` : ''}
                      </span>
                    </td>
                    <td className="py-3 px-4 font-black text-blue-400">
                      {formatPrice(p.lowestPriceUzs || p.priceUzs || 0)} <span className="text-[10px] font-semibold">so'm</span>
                    </td>
                    <td className="py-3 px-4 text-slate-300 font-bold">
                      {p.storeName || (p.offers && p.offers[0]?.store?.name) || 'Uzum Market'}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => openEditModal(p)}
                          className="p-2 text-blue-400 hover:bg-blue-950/60 rounded-xl transition-colors"
                          title="Tahrirlash"
                        >
                          <Edit className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => handleDelete(p.id)}
                          className="p-2 text-rose-400 hover:bg-rose-950/60 rounded-xl transition-colors"
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

      {/* Modal Form */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">
          <div className="bg-slate-900 rounded-3xl max-w-lg w-full p-6 shadow-2xl relative border border-slate-800 max-h-[90vh] overflow-y-auto">
            <button
              onClick={() => setIsModalOpen(false)}
              className="absolute top-4 right-4 text-slate-400 hover:text-white p-1 rounded-lg"
            >
              <X className="w-5 h-5" />
            </button>

            <h2 className="text-lg font-black text-white mb-4 flex items-center gap-2">
              <ShoppingBag className="w-5 h-5 text-blue-500" />
              <span>{editingProduct ? 'Mahsulotni Tahrirlash' : 'Yangi Mahsulot Qo\'shish'}</span>
            </h2>

            {formError && (
              <div className="p-3 mb-4 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-bold rounded-2xl">
                {formError}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4 text-xs font-bold">
              <div>
                <label className="block text-slate-300 mb-1">Mahsulot Nomi (Title / Name) *</label>
                <input
                  type="text"
                  required
                  value={titleUz}
                  onChange={(e) => setTitleUz(e.target.value)}
                  placeholder="masalan: iPhone 16 Pro Max / Samsung S24 / Asus ROG"
                  className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-white outline-none focus:border-blue-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-300 mb-1">Kategoriya</label>
                  <select
                    value={categoryId}
                    onChange={(e) => setCategoryId(Number(e.target.value))}
                    className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-white outline-none focus:border-blue-500"
                  >
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.nameUz}
                      </option>
                    ))}
                    <option value={999}>Boshqa / Yangi Kategoriya</option>
                  </select>
                </div>
                <div>
                  <label className="block text-slate-300 mb-1">Brend (Ixtiyoriy)</label>
                  <input
                    type="text"
                    value={brand}
                    onChange={(e) => setBrand(e.target.value)}
                    placeholder="Apple, Samsung, Nike, LG..."
                    className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-white outline-none focus:border-blue-500"
                  />
                </div>
              </div>

              {categoryId === 999 && (
                <div>
                  <label className="block text-slate-300 mb-1">Yangi Kategoriya Nomi</label>
                  <input
                    type="text"
                    value={customCategory}
                    onChange={(e) => setCustomCategory(e.target.value)}
                    placeholder="masalan: Avto-tovarlar / Kitoblar"
                    className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-white outline-none"
                  />
                </div>
              )}

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-slate-300 mb-1">Xotira (ixtiyoriy)</label>
                  <input
                    type="text"
                    value={storage}
                    onChange={(e) => setStorage(e.target.value)}
                    placeholder="256GB / 512GB"
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white outline-none"
                  />
                </div>
                <div>
                  <label className="block text-slate-300 mb-1">RAM (ixtiyoriy)</label>
                  <input
                    type="text"
                    value={ram}
                    onChange={(e) => setRam(e.target.value)}
                    placeholder="8GB / 16GB"
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white outline-none"
                  />
                </div>
                <div>
                  <label className="block text-slate-300 mb-1">Rangi (ixtiyoriy)</label>
                  <input
                    type="text"
                    value={color}
                    onChange={(e) => setColor(e.target.value)}
                    placeholder="Qora / Oq"
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-slate-300 mb-1">Tavsifi / Haqida (Description / About)</label>
                <textarea
                  rows={2}
                  value={descriptionUz}
                  onChange={(e) => setDescriptionUz(e.target.value)}
                  placeholder="Mahsulot haqida ma'lumot..."
                  className="w-full px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white outline-none font-medium"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-300 mb-1">Narxi (So'mda) *</label>
                  <input
                    type="number"
                    required
                    value={priceUzs}
                    onChange={(e) => setPriceUzs(e.target.value)}
                    placeholder="12500000"
                    className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-white outline-none font-black"
                  />
                </div>
                <div>
                  <label className="block text-slate-300 mb-1">Do'kon Nomi</label>
                  <input
                    type="text"
                    value={storeName}
                    onChange={(e) => setStoreName(e.target.value)}
                    placeholder="Uzum Market / Texnomart"
                    className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-white outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-slate-300 mb-1">Mahsulot Havolasi (Offer Link)</label>
                <input
                  type="url"
                  value={storeOfferUrl}
                  onChange={(e) => setStoreOfferUrl(e.target.value)}
                  placeholder="https://uzum.uz/product/..."
                  className="w-full px-3.5 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white outline-none font-medium"
                />
              </div>

              {/* Dual Image Input Switcher */}
              <div>
                <label className="block text-slate-300 mb-1.5">Mahsulot Rasmi</label>
                <div className="flex items-center gap-2 mb-2 bg-slate-950 p-1 rounded-xl border border-slate-800">
                  <button
                    type="button"
                    onClick={() => setImageMode('url')}
                    className={`flex-1 py-1.5 rounded-lg text-center font-extrabold transition-all flex items-center justify-center gap-1 ${
                      imageMode === 'url' ? 'bg-blue-600 text-white' : 'text-slate-400'
                    }`}
                  >
                    <LinkIcon className="w-3.5 h-3.5" />
                    <span>Ssilka Orqali</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setImageMode('upload')}
                    className={`flex-1 py-1.5 rounded-lg text-center font-extrabold transition-all flex items-center justify-center gap-1 ${
                      imageMode === 'upload' ? 'bg-blue-600 text-white' : 'text-slate-400'
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
                    className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-white outline-none"
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
                      className="w-full py-3 border-2 border-dashed border-slate-800 hover:border-blue-500 rounded-2xl flex items-center justify-center gap-2 text-slate-300 font-bold bg-slate-950 transition-colors"
                    >
                      <Upload className="w-4 h-4 text-blue-500" />
                      <span>{uploadingImage ? 'Rasm yuklanmoqda...' : 'Kompyuterdan rasm tanlang (.jpg, .png)'}</span>
                    </button>
                  </div>
                )}

                {imageUrl && (
                  <div className="mt-2 flex items-center gap-3 bg-slate-950 p-2 rounded-2xl border border-slate-800">
                    <img src={imageUrl} alt="Preview" className="w-12 h-12 object-contain rounded-lg bg-slate-900 p-1" />
                    <span className="text-[10px] text-slate-400 line-clamp-1 flex-1 font-mono">{imageUrl}</span>
                  </div>
                )}
              </div>

              <div className="flex items-center gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="flex-1 py-3 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-2xl font-bold"
                >
                  Bekor Qilish
                </button>
                <button
                  type="submit"
                  className="flex-1 py-3 bg-blue-600 hover:bg-blue-500 text-white rounded-2xl font-black shadow-lg shadow-blue-500/25"
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
