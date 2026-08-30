import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Lock, User, KeyRound, ArrowRight } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const Login: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      // 1. Try Backend REST API Authentication
      const API_BASE_URL = (import.meta as any).env?.VITE_API_BASE_URL || 'http://localhost:5001/api';
      const response = await fetch(`${API_BASE_URL}/admin/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username.trim(), password: password.trim() })
      });

      if (response.ok) {
        const data = await response.json();
        login(data.username || username, password);
        localStorage.setItem('priceiq_admin_token', data.token || 'admin-token');
        navigate('/');
        return;
      }
    } catch (err) {
      console.warn('Backend auth unreachable, trying fallback local auth check', err);
    } finally {
      setLoading(false);
    }

    // 2. Fallback local credentials check
    const success = login(username, password);
    if (success) {
      localStorage.setItem('priceiq_admin_token', 'admin-token-fallback');
      navigate('/');
    } else {
      setError('Login yoki parol noto\'g\'ri (Standart: admin / admin123)');
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex items-center justify-center p-4 relative overflow-hidden">
      {/* Glow Effects */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-blue-600/15 rounded-full blur-3xl pointer-events-none" />
      
      <div className="max-w-md w-full bg-slate-900 border border-slate-800 rounded-3xl p-8 shadow-2xl relative z-10 space-y-6">
        <div className="text-center space-y-2">
          <div className="w-14 h-14 bg-gradient-to-tr from-blue-600 to-indigo-600 text-white font-black text-2xl rounded-2xl flex items-center justify-center mx-auto shadow-lg shadow-blue-500/25 border border-white/10">
            IQ
          </div>
          <h1 className="text-2xl font-black text-white tracking-tight">
            PRICEIQ Admin Portal
          </h1>
          <p className="text-xs text-slate-400 font-medium">
            Smartfonlar va do'kon narxlarini boshqarish tizimiga kirish
          </p>
        </div>

        {error && (
          <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-bold rounded-2xl text-center">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4 text-xs font-bold">
          <div>
            <label className="block text-slate-300 mb-1.5">Foydalanuvchi Nomi (Username)</label>
            <div className="relative">
              <input
                type="text"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="admin"
                className="w-full pl-10 pr-4 py-3 bg-slate-950 border border-slate-800 rounded-2xl text-white outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
              />
              <User className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
            </div>
          </div>

          <div>
            <label className="block text-slate-300 mb-1.5">Maxfiy Parol (Password)</label>
            <div className="relative">
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full pl-10 pr-4 py-3 bg-slate-950 border border-slate-800 rounded-2xl text-white outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
              />
              <Lock className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
            </div>
          </div>

          <div className="bg-slate-950/80 p-3 rounded-2xl border border-slate-800 text-[11px] text-slate-400 font-mono flex items-center gap-2">
            <KeyRound className="w-4 h-4 text-amber-400 shrink-0" />
            <span>Standart Login: <b>admin</b> | Parol: <b>admin123</b></span>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3.5 bg-blue-600 hover:bg-blue-500 text-white rounded-2xl font-black text-xs flex items-center justify-center gap-2 shadow-lg shadow-blue-500/25 transition-all active:scale-[0.99] disabled:opacity-50"
          >
            <span>{loading ? 'Kirilmoqda...' : 'Tizimga Kirish'}</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </form>
      </div>
    </div>
  );
};
