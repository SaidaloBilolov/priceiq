import React, { useState, useEffect } from 'react';
import { Store as StoreIcon, Plus, Phone, Globe, Star, Trash2, Edit2, CheckCircle2, AlertCircle, X, Search } from 'lucide-react';
import { Store } from '../types';
import { api } from '../services/api';

export const Stores: React.FC = () => {
  const [stores, setStores] = useState<Store[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [search, setSearch] = useState<string>('');
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [editingStore, setEditingStore] = useState<Store | null>(null);

  const [formData, setFormData] = useState<Partial<Store>>({
    name: '',
    ownerPhone: '+998',
    websiteUrl: 'https://',
    rating: 4.8,
    logoUrl: 'https://images.unsplash.com/photo-1526738549149-8e07eca6c147?auto=format&fit=crop&w=120&q=80'
  });

  const [message, setMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  const loadStores = async () => {
    setLoading(true);
    try {
      const data = await api.getStores();
      setStores(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStores();
  }, []);

  const openAddModal = () => {
    setEditingStore(null);
    setFormData({
      name: '',
      ownerPhone: '+998',
      websiteUrl: 'https://',
      rating: 4.8,
      logoUrl: 'https://images.unsplash.com/photo-1526738549149-8e07eca6c147?auto=format&fit=crop&w=120&q=80'
    });
    setIsModalOpen(true);
  };

  const openEditModal = (store: Store) => {
    setEditingStore(store);
    setFormData({
      name: store.name,
      ownerPhone: store.ownerPhone || '+998',
      websiteUrl: store.websiteUrl || 'https://',
      rating: store.rating || 4.8,
      logoUrl: store.logoUrl || 'https://images.unsplash.com/photo-1526738549149-8e07eca6c147?auto=format&fit=crop&w=120&q=80'
    });
    setIsModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name?.trim()) {
      setMessage({ text: 'Iltimos, do\'kon nomini kiriting', type: 'error' });
      return;
    }

    try {
      if (editingStore) {
        const updated = await api.updateStore(editingStore.id, formData);
        setStores(prev => prev.map(s => s.id === editingStore.id ? updated : s));
        setMessage({ text: 'Do\'kon ma\'lumotlari muvaffaqiyatli yangilandi!', type: 'success' });
      } else {
        const created = await api.createStore(formData);
        setStores(prev => [created, ...prev]);
        setMessage({ text: 'Yangi do\'kon muvaffaqiyatli qo\'shildi va sotuvchi raqami biriktirildi!', type: 'success' });
      }
      setIsModalOpen(false);
      setTimeout(() => setMessage(null), 4000);
    } catch (e) {
      setMessage({ text: 'Xatolik yuz berdi. Qayta urinib ko\'ring', type: 'error' });
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Haqiqatdan ham ushbu do\'konni o\'chirmoqchimisiz?')) return;
    try {
      await api.deleteStore(id);
      setStores(prev => prev.filter(s => s.id !== id));
      setMessage({ text: 'Do\'kon o\'chirildi', type: 'success' });
      setTimeout(() => setMessage(null), 3000);
    } catch (e) {
      setMessage({ text: 'O\'chirishda xatolik yuz berdi', type: 'error' });
    }
  };

  const filteredStores = stores.filter(s =>
    s.name.toLowerCase().includes(search.toLowerCase()) ||
    (s.ownerPhone || '').includes(search)
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-gray-900 border border-gray-800 p-6 rounded-2xl shadow-xl">
        <div>
          <h1 className="text-2xl font-black text-white flex items-center gap-3">
            <StoreIcon className="w-7 h-7 text-blue-500" />
            Do'konlar & Sotuvchilar Boshqaruvi
          </h1>
          <p className="text-gray-400 text-xs mt-1">
            Yangi do'kon qo'shing va Telegram Bot orqali mahsulot kirituvchi sotuvchi telefon raqamini biriktiring
          </p>
        </div>

        <button
          onClick={openAddModal}
          className="flex items-center justify-center gap-2 px-5 py-3 bg-blue-600 hover:bg-blue-700 text-white text-xs font-black rounded-xl transition-all shadow-lg shadow-blue-500/20"
        >
          <Plus className="w-4 h-4" />
          Yangi Do'kon Qo'shish
        </button>
      </div>

      {message && (
        <div className={`p-4 rounded-xl flex items-center gap-3 text-xs font-bold ${
          message.type === 'success' ? 'bg-green-500/10 text-green-400 border border-green-500/20' : 'bg-red-500/10 text-red-400 border border-red-500/20'
        }`}>
          {message.type === 'success' ? <CheckCircle2 className="w-5 h-5" /> : <AlertCircle className="w-5 h-5" />}
          {message.text}
        </div>
      )}

      {/* Search Bar */}
      <div className="relative">
        <input
          type="text"
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="Do'kon nomi yoki sotuvchi telefon raqami bo'yicha qidirish..."
          className="w-full pl-10 pr-4 py-3 bg-gray-900 border border-gray-800 rounded-xl text-white text-xs placeholder-gray-500 focus:outline-none focus:border-blue-500 transition-colors"
        />
        <Search className="w-4 h-4 text-gray-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
      </div>

      {/* Stores List */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {loading ? (
          <div className="col-span-full py-12 text-center text-gray-500 text-xs">Yuklanmoqda...</div>
        ) : filteredStores.length === 0 ? (
          <div className="col-span-full py-12 text-center text-gray-500 text-xs">Hech qanday do'kon topilmadi</div>
        ) : (
          filteredStores.map(store => (
            <div key={store.id} className="bg-gray-900 border border-gray-800 rounded-2xl p-5 hover:border-gray-700 transition-all space-y-4">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-xl bg-blue-600/10 border border-blue-500/20 flex items-center justify-center text-blue-400 font-black text-lg">
                    {store.name.substring(0, 2).toUpperCase()}
                  </div>
                  <div>
                    <h3 className="text-white font-bold text-sm">{store.name}</h3>
                    <div className="flex items-center gap-1 text-yellow-400 text-xs mt-0.5">
                      <Star className="w-3.5 h-3.5 fill-current" />
                      <span>{store.rating || 4.8}</span>
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-1">
                  <button
                    onClick={() => openEditModal(store)}
                    className="p-2 text-gray-400 hover:text-blue-400 rounded-lg hover:bg-gray-800 transition-colors"
                  >
                    <Edit2 className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => handleDelete(store.id)}
                    className="p-2 text-gray-400 hover:text-red-400 rounded-lg hover:bg-gray-800 transition-colors"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>

              <div className="space-y-2 pt-2 border-t border-gray-800 text-xs">
                <div className="flex items-center justify-between">
                  <span className="text-gray-500 flex items-center gap-1.5">
                    <Phone className="w-3.5 h-3.5 text-green-400" />
                    Sotuvchi Tel:
                  </span>
                  <span className="text-green-400 font-bold bg-green-500/10 px-2 py-0.5 rounded border border-green-500/20">
                    {store.ownerPhone || 'Biriktirilmagan'}
                  </span>
                </div>

                {store.websiteUrl && (
                  <div className="flex items-center justify-between">
                    <span className="text-gray-500 flex items-center gap-1.5">
                      <Globe className="w-3.5 h-3.5 text-blue-400" />
                      Veb-sayt:
                    </span>
                    <a
                      href={store.websiteUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="text-blue-400 hover:underline truncate max-w-[150px]"
                    >
                      {store.websiteUrl.replace(/^https?:\/\//, '')}
                    </a>
                  </div>
                )}

                <div className="flex items-center justify-between pt-1">
                  <span className="text-gray-500">Bot Ulanish Holati:</span>
                  <span className={`px-2 py-0.5 rounded text-[10px] font-black ${
                    store.ownerPhone ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20' : 'bg-gray-800 text-gray-400'
                  }`}>
                    {store.ownerPhone ? 'Telegram Bot Faol' : 'Kutilmoqda'}
                  </span>
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Add / Edit Store Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
          <div className="bg-gray-900 border border-gray-800 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-5">
            <div className="flex items-center justify-between pb-3 border-b border-gray-800">
              <h3 className="text-white font-bold text-base flex items-center gap-2">
                <StoreIcon className="w-5 h-5 text-blue-500" />
                {editingStore ? "Do'konni Tahrirlash" : "Yangi Do'kon Qo'shish"}
              </h3>
              <button
                onClick={() => setIsModalOpen(false)}
                className="text-gray-400 hover:text-white p-1 rounded-lg hover:bg-gray-800"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4 text-xs">
              <div>
                <label className="block text-gray-400 font-bold mb-1.5">Do'kon Nomi *</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={e => setFormData({ ...formData, name: e.target.value })}
                  placeholder="Masalan: Anor Texnika / MediaPark"
                  required
                  className="w-full px-3.5 py-2.5 bg-gray-800 border border-gray-700 rounded-xl text-white focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-gray-400 font-bold mb-1.5">
                  Sotuvchi Telefon Raqami (Telegram Bot uchun) *
                </label>
                <input
                  type="text"
                  value={formData.ownerPhone}
                  onChange={e => setFormData({ ...formData, ownerPhone: e.target.value })}
                  placeholder="+998901234567"
                  required
                  className="w-full px-3.5 py-2.5 bg-gray-800 border border-gray-700 rounded-xl text-green-400 font-bold focus:outline-none focus:border-blue-500"
                />
                <p className="text-gray-500 text-[11px] mt-1">
                  Ushbu raqam egasi Telegram botga kirib raqamini yuborganda, bot darhol unga mahsulot qo'shish menyusini ochadi.
                </p>
              </div>

              <div>
                <label className="block text-gray-400 font-bold mb-1.5">Do'kon Veb-sayti / Havolasi</label>
                <input
                  type="text"
                  value={formData.websiteUrl}
                  onChange={e => setFormData({ ...formData, websiteUrl: e.target.value })}
                  placeholder="https://uzum.uz yoki https://olcha.uz"
                  className="w-full px-3.5 py-2.5 bg-gray-800 border border-gray-700 rounded-xl text-white focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-gray-400 font-bold mb-1.5">Reyting (1.0 - 5.0)</label>
                <input
                  type="number"
                  step="0.1"
                  min="1"
                  max="5"
                  value={formData.rating}
                  onChange={e => setFormData({ ...formData, rating: parseFloat(e.target.value) || 4.8 })}
                  className="w-full px-3.5 py-2.5 bg-gray-800 border border-gray-700 rounded-xl text-white focus:outline-none focus:border-blue-500"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-gray-800">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2.5 bg-gray-800 hover:bg-gray-700 text-gray-300 font-bold rounded-xl"
                >
                  Bekor qilish
                </button>
                <button
                  type="submit"
                  className="px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-black rounded-xl shadow-lg shadow-blue-500/20"
                >
                  {editingStore ? "Saqlash" : "Qo'shish"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
