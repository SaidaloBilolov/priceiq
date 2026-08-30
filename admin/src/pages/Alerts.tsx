import React, { useState, useEffect } from 'react';
import { Bell, RefreshCw, Smartphone } from 'lucide-react';
import { PriceAlert } from '../types';
import { api } from '../services/api';

export const Alerts: React.FC = () => {
  const [alerts, setAlerts] = useState<PriceAlert[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchAlerts = async () => {
    setLoading(true);
    try {
      const data = await api.getPriceAlerts();
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

  const formatPrice = (amount: number) => {
    return new Intl.NumberFormat('fr-FR').format(amount).replace(/,/g, ' ');
  };

  return (
    <div className="space-y-6 pb-20">
      <div className="flex items-center justify-between bg-slate-900 p-6 rounded-3xl border border-slate-800 shadow-xl">
        <div>
          <h1 className="text-xl font-black text-white tracking-tight flex items-center gap-2">
            <Bell className="w-5 h-5 text-blue-500" />
            <span>Foydalanuvchilar Narx Alertlari</span>
          </h1>
          <p className="text-xs text-slate-400 mt-1">
            Telegram foydalanuvchilari tomonidan o'rnatilgan maqsadli narxlar monitoringi
          </p>
        </div>

        <button
          onClick={fetchAlerts}
          className="p-2.5 bg-slate-800 text-slate-400 hover:text-white rounded-2xl border border-slate-700 transition-colors"
        >
          <RefreshCw className="w-4 h-4" />
        </button>
      </div>

      <div className="bg-slate-900 rounded-3xl border border-slate-800 shadow-xl p-6">
        {loading ? (
          <div className="flex justify-center py-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
          </div>
        ) : alerts.length === 0 ? (
          <div className="text-center py-12">
            <Bell className="w-12 h-12 text-slate-600 mx-auto mb-3" />
            <p className="text-sm font-bold text-slate-400">Faol alertlar mavjud emas</p>
          </div>
        ) : (
          <div className="space-y-3">
            {alerts.map((a) => (
              <div key={a.id} className="bg-slate-950 p-4 rounded-2xl border border-slate-800 flex items-center justify-between gap-3">
                <div className="flex items-center gap-3">
                  <img
                    src={a.product?.imageUrl}
                    alt={a.product?.titleUz}
                    className="w-10 h-10 object-contain rounded-lg bg-slate-900 p-1"
                  />
                  <div>
                    <h3 className="text-xs font-bold text-white line-clamp-1">{a.product?.titleUz}</h3>
                    <span className="text-[10px] text-slate-400 font-medium">User Telegram ID: {a.userId}</span>
                  </div>
                </div>

                <div className="text-right">
                  <span className="text-xs font-black text-blue-400 block">{formatPrice(a.targetPriceUzs)} so'm</span>
                  <span className="text-[9px] text-emerald-400 font-bold uppercase">MAQSADLI NARX</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
