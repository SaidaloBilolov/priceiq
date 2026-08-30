import React, { useState } from 'react';
import { Settings, ExternalLink, LogOut, Server, Check } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { getApiBaseUrl, setCustomApiBaseUrl } from '../services/api';

export const Navbar: React.FC = () => {
  const { logout } = useAuth();
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);
  const [apiUrl, setApiUrl] = useState(getApiBaseUrl());
  const [savedMsg, setSavedMsg] = useState('');

  const handleSaveSettings = (e: React.FormEvent) => {
    e.preventDefault();
    setCustomApiBaseUrl(apiUrl);
    setSavedMsg('API Server URL muvaffaqiyatli saqlandi!');
    setTimeout(() => {
      setSavedMsg('');
      setIsSettingsOpen(false);
      window.location.reload();
    }, 1200);
  };

  return (
    <header className="sticky top-0 z-40 bg-slate-900/90 backdrop-blur-md border-b border-slate-800 px-6 py-3.5 flex items-center justify-between">
      <div className="flex items-center gap-2">
        <span className="px-2.5 py-1 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 rounded-lg text-[10px] font-black uppercase tracking-wider">
          NEON POSTGRES CONNECTED
        </span>
        <span className="text-xs text-slate-400 font-mono hidden sm:inline truncate max-w-xs">
          {getApiBaseUrl()}
        </span>
      </div>

      <div className="flex items-center gap-2">
        {/* Settings button */}
        <button
          onClick={() => setIsSettingsOpen(true)}
          className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-xs font-bold flex items-center gap-1.5 transition-colors border border-slate-700"
          title="Backend API Sozlamalari"
        >
          <Settings className="w-3.5 h-3.5 text-blue-400" />
          <span className="hidden sm:inline">API Sozlamalari</span>
        </button>

        <a
          href="https://frontend-three-gamma-ca7l713sls.vercel.app"
          target="_blank"
          rel="noopener noreferrer"
          className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-xs font-bold flex items-center gap-1.5 transition-colors border border-slate-700"
        >
          <span className="hidden sm:inline">Mini App</span>
          <ExternalLink className="w-3.5 h-3.5" />
        </a>

        <button
          onClick={logout}
          className="md:hidden p-2 text-rose-400 hover:bg-rose-950/40 rounded-xl"
          title="Chiqish"
        >
          <LogOut className="w-4 h-4" />
        </button>
      </div>

      {/* Settings Modal */}
      {isSettingsOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-fadeIn">
          <div className="bg-slate-900 rounded-3xl max-w-md w-full p-6 shadow-2xl relative border border-slate-800 space-y-4">
            <h2 className="text-lg font-black text-white flex items-center gap-2">
              <Server className="w-5 h-5 text-blue-500" />
              <span>Backend API Server Sozlamalari</span>
            </h2>
            <p className="text-xs text-slate-400">
              Vercel domenidan backend REST API serveringizga ulanish manzilini sozlang.
            </p>

            {savedMsg && (
              <div className="p-3 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-xs font-bold rounded-2xl flex items-center gap-2">
                <Check className="w-4 h-4" />
                <span>{savedMsg}</span>
              </div>
            )}

            <form onSubmit={handleSaveSettings} className="space-y-4 text-xs font-bold">
              <div>
                <label className="block text-slate-300 mb-1.5">Backend REST API URL</label>
                <input
                  type="text"
                  required
                  value={apiUrl}
                  onChange={(e) => setApiUrl(e.target.value)}
                  placeholder="http://localhost:5001/api"
                  className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-white outline-none focus:border-blue-500 font-mono"
                />
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setApiUrl('http://localhost:5001/api')}
                  className="px-3 py-1.5 bg-slate-950 text-slate-400 hover:text-white rounded-lg border border-slate-800 text-[11px]"
                >
                  Localhost (5001)
                </button>
              </div>

              <div className="flex items-center gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setIsSettingsOpen(false)}
                  className="flex-1 py-2.5 bg-slate-800 text-slate-300 rounded-xl font-bold"
                >
                  Bekor qilish
                </button>
                <button
                  type="submit"
                  className="flex-1 py-2.5 bg-blue-600 text-white rounded-xl font-black shadow-lg shadow-blue-500/25"
                >
                  Saqlash
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </header>
  );
};
