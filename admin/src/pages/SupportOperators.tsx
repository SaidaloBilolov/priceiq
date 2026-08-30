import React, { useState, useEffect } from 'react';
import { Headphones, Plus, Phone, CheckCircle2, AlertCircle, Trash2, Edit2, X, Search, ShieldCheck, MessageSquare, Clock, User, Reply, ArrowRight, RefreshCw } from 'lucide-react';
import { SupportOperator, SupportTicket } from '../types';
import { api } from '../services/api';

export const SupportOperators: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'operators' | 'tickets'>('operators');
  const [operators, setOperators] = useState<SupportOperator[]>([]);
  const [tickets, setTickets] = useState<SupportTicket[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [ticketsLoading, setTicketsLoading] = useState<boolean>(false);
  const [search, setSearch] = useState<string>('');
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [editingOperator, setEditingOperator] = useState<SupportOperator | null>(null);

  const [formData, setFormData] = useState<Partial<SupportOperator>>({
    fullName: '',
    phoneNumber: '+998',
    isActive: true
  });

  const [message, setMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  const loadOperators = async () => {
    setLoading(true);
    try {
      const data = await api.getSupportOperators();
      setOperators(data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const loadTickets = async () => {
    setTicketsLoading(true);
    try {
      const data = await api.getSupportTickets();
      setTickets(data);
    } catch (e) {
      console.error(e);
    } finally {
      setTicketsLoading(false);
    }
  };

  useEffect(() => {
    loadOperators();
    loadTickets();
    const interval = setInterval(loadTickets, 8000); // Polling tickets every 8s
    return () => clearInterval(interval);
  }, []);

  const openAddModal = () => {
    setEditingOperator(null);
    setFormData({
      fullName: '',
      phoneNumber: '+998',
      isActive: true
    });
    setIsModalOpen(true);
  };

  const openEditModal = (op: SupportOperator) => {
    setEditingOperator(op);
    setFormData({
      fullName: op.fullName,
      phoneNumber: op.phoneNumber,
      isActive: op.isActive
    });
    setIsModalOpen(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.fullName?.trim() || !formData.phoneNumber?.trim()) {
      setMessage({ text: 'Iltimos, barcha maydonlarni to\'ldiring', type: 'error' });
      return;
    }

    try {
      if (editingOperator) {
        const updated = await api.updateSupportOperator(editingOperator.id, formData);
        setOperators(prev => prev.map(o => o.id === editingOperator.id ? updated : o));
        setMessage({ text: 'Operator ma\'lumotlari yangilandi!', type: 'success' });
      } else {
        const created = await api.createSupportOperator(formData);
        setOperators(prev => [created, ...prev]);
        setMessage({ text: 'Yangi support operator muvaffaqiyatli qo\'shildi!', type: 'success' });
      }
      setIsModalOpen(false);
      setTimeout(() => setMessage(null), 4000);
    } catch (e) {
      setMessage({ text: 'Xatolik yuz berdi', type: 'error' });
    }
  };

  const toggleStatus = async (op: SupportOperator) => {
    try {
      const updated = await api.updateSupportOperator(op.id, { isActive: !op.isActive });
      setOperators(prev => prev.map(o => o.id === op.id ? updated : o));
      setMessage({ text: `Operator holati: ${!op.isActive ? 'Faollashtirildi' : 'Faolsizlantirildi'}`, type: 'success' });
      setTimeout(() => setMessage(null), 3000);
    } catch (e) {
      setMessage({ text: 'Holatni o\'zgartirishda xatolik', type: 'error' });
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Haqiqatdan ham ushbu operatorni o\'chirmoqchimisiz?')) return;
    try {
      await api.deleteSupportOperator(id);
      setOperators(prev => prev.filter(o => o.id !== id));
      setMessage({ text: 'Operator o\'chirildi', type: 'success' });
      setTimeout(() => setMessage(null), 3000);
    } catch (e) {
      setMessage({ text: 'O\'chirishda xatolik', type: 'error' });
    }
  };

  const filteredOperators = operators.filter(o =>
    o.fullName.toLowerCase().includes(search.toLowerCase()) ||
    o.phoneNumber.includes(search)
  );

  const filteredTickets = tickets.filter(t =>
    (t.userName && t.userName.toLowerCase().includes(search.toLowerCase())) ||
    (t.userPhone && t.userPhone.includes(search)) ||
    (t.messageText && t.messageText.toLowerCase().includes(search.toLowerCase())) ||
    (t.operatorName && t.operatorName.toLowerCase().includes(search.toLowerCase()))
  );

  const pendingCount = tickets.filter(t => t.status === 'PENDING').length;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-gray-900 border border-gray-800 p-6 rounded-2xl shadow-xl">
        <div>
          <h1 className="text-2xl font-black text-white flex items-center gap-3">
            <Headphones className="w-7 h-7 text-indigo-400" />
            Support & Operatorlar Boshqaruvi
          </h1>
          <p className="text-gray-400 text-xs mt-1">
            Support operatorlarni boshqaring va foydalanuvchilar bilan bo'lgan barcha yozishmalar tarixini kuzatib boring
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => { loadOperators(); loadTickets(); }}
            title="Yangilash"
            className="p-3 bg-gray-800 hover:bg-gray-700 text-gray-300 rounded-xl transition-colors border border-gray-700"
          >
            <RefreshCw className="w-4 h-4" />
          </button>

          {activeTab === 'operators' && (
            <button
              onClick={openAddModal}
              className="flex items-center justify-center gap-2 px-5 py-3 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-black rounded-xl transition-all shadow-lg shadow-indigo-500/20"
            >
              <Plus className="w-4 h-4" />
              Yangi Operator Qo'shish
            </button>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-3 border-b border-gray-800 pb-2">
        <button
          onClick={() => setActiveTab('operators')}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-black transition-all ${
            activeTab === 'operators'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-500/20'
              : 'bg-gray-900 text-gray-400 hover:text-white border border-gray-800'
          }`}
        >
          <Headphones className="w-4 h-4" />
          Operatorlar Ro'yxati ({operators.length})
        </button>

        <button
          onClick={() => setActiveTab('tickets')}
          className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs font-black transition-all ${
            activeTab === 'tickets'
              ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-500/20'
              : 'bg-gray-900 text-gray-400 hover:text-white border border-gray-800'
          }`}
        >
          <MessageSquare className="w-4 h-4" />
          Murojaatlar & Yozishmalar Tarixi ({tickets.length})
          {pendingCount > 0 && (
            <span className="px-1.5 py-0.5 bg-yellow-500 text-black text-[10px] font-black rounded-full animate-pulse">
              {pendingCount}
            </span>
          )}
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
          placeholder={activeTab === 'operators' ? "Operator ismi yoki telefoni bo'yicha qidirish..." : "Murojaat matni, foydalanuvchi yoki operator bo'yicha qidirish..."}
          className="w-full pl-10 pr-4 py-3 bg-gray-900 border border-gray-800 rounded-xl text-white text-xs placeholder-gray-500 focus:outline-none focus:border-indigo-500 transition-colors"
        />
        <Search className="w-4 h-4 text-gray-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
      </div>

      {/* TAB 1: Operators List */}
      {activeTab === 'operators' && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {loading ? (
            <div className="col-span-full py-12 text-center text-gray-500 text-xs">Yuklanmoqda...</div>
          ) : filteredOperators.length === 0 ? (
            <div className="col-span-full py-12 text-center text-gray-500 text-xs">Hech qanday support operator topilmadi</div>
          ) : (
            filteredOperators.map(op => (
              <div key={op.id} className={`bg-gray-900 border ${op.isActive ? 'border-gray-800' : 'border-red-900/40 opacity-75'} rounded-2xl p-5 hover:border-gray-700 transition-all space-y-4`}>
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 rounded-xl bg-indigo-600/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400 font-black text-lg">
                      {op.fullName.substring(0, 2).toUpperCase()}
                    </div>
                    <div>
                      <h3 className="text-white font-bold text-sm flex items-center gap-1.5">
                        {op.fullName}
                        {op.telegramChatId && <ShieldCheck className="w-4 h-4 text-green-400" />}
                      </h3>
                      <span className="text-gray-400 text-xs flex items-center gap-1 mt-0.5">
                        <Phone className="w-3 h-3 text-indigo-400" />
                        {op.phoneNumber}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => toggleStatus(op)}
                      title={op.isActive ? "Faolsizlantirish" : "Faollashtirish"}
                      className={`p-2 rounded-lg transition-colors ${op.isActive ? 'text-green-400 hover:bg-green-500/10' : 'text-gray-500 hover:bg-gray-800'}`}
                    >
                      <CheckCircle2 className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => openEditModal(op)}
                      className="p-2 text-gray-400 hover:text-indigo-400 rounded-lg hover:bg-gray-800 transition-colors"
                    >
                      <Edit2 className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handleDelete(op.id)}
                      className="p-2 text-gray-400 hover:text-red-400 rounded-lg hover:bg-gray-800 transition-colors"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                <div className="space-y-2 pt-2 border-t border-gray-800 text-xs">
                  <div className="flex items-center justify-between">
                    <span className="text-gray-500">Telegram Bot Ulanish:</span>
                    <span className={`px-2 py-0.5 rounded text-[10px] font-black ${
                      op.telegramChatId ? 'bg-green-500/10 text-green-400 border border-green-500/20' : 'bg-yellow-500/10 text-yellow-400 border border-yellow-500/20'
                    }`}>
                      {op.telegramChatId ? `Faol (ID: ${op.telegramChatId})` : 'Kutilmoqda (/start kuting)'}
                    </span>
                  </div>

                  <div className="flex items-center justify-between">
                    <span className="text-gray-500">Murojaatlarni Qabul Qilish:</span>
                    <span className={`px-2 py-0.5 rounded text-[10px] font-black ${
                      op.isActive ? 'bg-indigo-500/10 text-indigo-400 border border-indigo-500/20' : 'bg-red-500/10 text-red-400 border border-red-500/20'
                    }`}>
                      {op.isActive ? 'Yoqilgan (ON)' : 'O\'chirilgan (OFF)'}
                    </span>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* TAB 2: Tickets & Messages History */}
      {activeTab === 'tickets' && (
        <div className="space-y-4">
          {ticketsLoading && tickets.length === 0 ? (
            <div className="py-12 text-center text-gray-500 text-xs">Murojaatlar yuklanmoqda...</div>
          ) : filteredTickets.length === 0 ? (
            <div className="py-12 text-center text-gray-500 text-xs">Hozircha hech qanday support murojaati mavjud emas</div>
          ) : (
            filteredTickets.map(t => (
              <div key={t.id} className="bg-gray-900 border border-gray-800 rounded-2xl p-5 hover:border-gray-700 transition-all space-y-4 shadow-lg">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 pb-3 border-b border-gray-800 text-xs">
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-xl bg-indigo-600/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400 font-bold text-sm">
                      <User className="w-4 h-4" />
                    </div>
                    <div>
                      <div className="font-bold text-white flex items-center gap-2">
                        {t.userName || "Foydalanuvchi"}
                        <span className="text-[10px] px-2 py-0.5 bg-gray-800 text-gray-300 rounded font-medium border border-gray-700">
                          {t.userRole || "Xaridor"}
                        </span>
                      </div>
                      <div className="text-gray-400 text-[11px] flex items-center gap-2 mt-0.5">
                        <span className="flex items-center gap-1"><Phone className="w-3 h-3 text-indigo-400" /> {t.userPhone || "Ko'rsatilmagan"}</span>
                        <span>•</span>
                        <span>Chat ID: <code className="text-gray-300">{t.userChatId}</code></span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <span className={`px-2.5 py-1 rounded-lg text-[10px] font-black border ${
                      t.status === 'ANSWERED'
                        ? 'bg-green-500/10 text-green-400 border-green-500/20'
                        : 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20 animate-pulse'
                    }`}>
                      {t.status === 'ANSWERED' ? '✅ Javob berilgan' : '⏳ Kutilmoqda'}
                    </span>
                    <span className="text-gray-500 text-[11px] flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      {t.createdAt ? new Date(t.createdAt).toLocaleString() : ''}
                    </span>
                  </div>
                </div>

                {/* User Message */}
                <div className="bg-gray-950/60 p-4 rounded-xl border border-gray-800/80 text-xs space-y-1">
                  <span className="text-[10px] font-bold text-indigo-400 uppercase tracking-wider">Foydalanuvchi Murojaati:</span>
                  <p className="text-gray-200 leading-relaxed whitespace-pre-wrap font-medium">{t.messageText}</p>
                  {t.mediaType && t.mediaType !== 'TEXT' && (
                    <span className="inline-block mt-2 px-2 py-0.5 bg-gray-800 text-indigo-300 rounded text-[10px] font-bold border border-gray-700">
                      📎 Media turi: {t.mediaType}
                    </span>
                  )}
                </div>

                {/* Operator Response */}
                {t.replyText ? (
                  <div className="bg-green-950/20 border border-green-500/20 p-4 rounded-xl text-xs space-y-1.5">
                    <div className="flex items-center justify-between text-[11px]">
                      <span className="text-green-400 font-bold flex items-center gap-1.5">
                        <Reply className="w-3.5 h-3.5" />
                        Operator Javobi: <strong className="text-white">{t.operatorName || "Support Operator"}</strong>
                      </span>
                      <span className="text-gray-500 text-[10px]">
                        {t.repliedAt ? new Date(t.repliedAt).toLocaleString() : ''}
                      </span>
                    </div>
                    <p className="text-green-200/90 whitespace-pre-wrap leading-relaxed">{t.replyText}</p>
                  </div>
                ) : (
                  <div className="flex items-center gap-2 text-xs text-yellow-400/80 bg-yellow-500/5 border border-yellow-500/10 p-3 rounded-xl">
                    <AlertCircle className="w-4 h-4 shrink-0" />
                    <span>Ushbu murojaatga operatorlarimiz tomonidan hali javob yozilmagan. Operator Telegram botda xabarga 'Reply' qilib javob berishi mumkin.</span>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      )}

      {/* Add / Edit Operator Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
          <div className="bg-gray-900 border border-gray-800 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-5">
            <div className="flex items-center justify-between pb-3 border-b border-gray-800">
              <h3 className="text-white font-bold text-base flex items-center gap-2">
                <Headphones className="w-5 h-5 text-indigo-400" />
                {editingOperator ? "Operatorni Tahrirlash" : "Yangi Support Operator"}
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
                <label className="block text-gray-400 font-bold mb-1.5">F.I.SH (Operator Ismi) *</label>
                <input
                  type="text"
                  value={formData.fullName}
                  onChange={e => setFormData({ ...formData, fullName: e.target.value })}
                  placeholder="Masalan: Super Admin"
                  required
                  className="w-full px-3.5 py-2.5 bg-gray-800 border border-gray-700 rounded-xl text-white focus:outline-none focus:border-indigo-500"
                />
              </div>

              <div>
                <label className="block text-gray-400 font-bold mb-1.5">
                  Operator Telefon Raqami (Telegram uchun) *
                </label>
                <input
                  type="text"
                  value={formData.phoneNumber}
                  onChange={e => setFormData({ ...formData, phoneNumber: e.target.value })}
                  placeholder="+998956233923"
                  required
                  className="w-full px-3.5 py-2.5 bg-gray-800 border border-gray-700 rounded-xl text-green-400 font-bold focus:outline-none focus:border-indigo-500"
                />
                <p className="text-gray-500 text-[11px] mt-1">
                  Ushbu raqam egasi botga kirib raqamini yuborganda, bot uni Support Operator sifatida ro'yxatdan o'tkazadi va foydalanuvchilar murojaatlarini unga yetkazadi.
                </p>
              </div>

              <div className="flex items-center gap-2 pt-2">
                <input
                  type="checkbox"
                  id="isActive"
                  checked={formData.isActive}
                  onChange={e => setFormData({ ...formData, isActive: e.target.checked })}
                  className="w-4 h-4 rounded bg-gray-800 border-gray-700 text-indigo-600 focus:ring-indigo-500"
                />
                <label htmlFor="isActive" className="text-gray-300 font-bold">
                  Operator hozir faol (murojaatlarni qabul qilsin)
                </label>
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
                  className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-black rounded-xl shadow-lg shadow-indigo-500/20"
                >
                  {editingOperator ? "Saqlash" : "Qo'shish"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
