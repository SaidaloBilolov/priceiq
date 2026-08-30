import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Smartphone, Bell, Shield, LogOut } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export const Sidebar: React.FC = () => {
  const { logout, user } = useAuth();

  const navItems = [
    { path: '/', label: 'Boshqaruv Paneli', icon: LayoutDashboard },
    { path: '/products', label: 'Mahsulotlar CRUD', icon: Smartphone },
    { path: '/alerts', label: 'Narx Alertlari', icon: Bell }
  ];

  return (
    <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col justify-between p-4 shrink-0 hidden md:flex">
      <div className="space-y-6">
        {/* Logo */}
        <div className="flex items-center gap-3 px-2 py-2">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center text-white font-black text-xl shadow-lg border border-white/10">
            IQ
          </div>
          <div>
            <h1 className="font-black text-lg text-white tracking-tight leading-none">
              PRICE<span className="text-blue-500">IQ</span>
            </h1>
            <span className="text-[10px] font-extrabold text-blue-400 uppercase tracking-widest block mt-0.5">
              Admin Portal
            </span>
          </div>
        </div>

        {/* User Card */}
        <div className="bg-slate-850 bg-slate-800/60 p-3 rounded-2xl border border-slate-750 border-slate-700/60 flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-blue-600 text-white font-bold text-sm flex items-center justify-center">
            A
          </div>
          <div className="overflow-hidden">
            <h2 className="text-xs font-bold text-white truncate">{user?.name || 'Administrator'}</h2>
            <span className="text-[10px] text-emerald-400 font-semibold flex items-center gap-1">
              <Shield className="w-3 h-3" /> Super Admin
            </span>
          </div>
        </div>

        {/* Navigation Links */}
        <nav className="space-y-1.5 pt-2">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-4 py-3 rounded-2xl text-xs font-extrabold transition-all ${
                    isActive
                      ? 'bg-blue-600 text-white shadow-lg shadow-blue-500/25'
                      : 'text-slate-400 hover:text-white hover:bg-slate-800/80'
                  }`
                }
              >
                <Icon className="w-4 h-4" />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>
      </div>

      {/* Logout */}
      <button
        onClick={logout}
        className="flex items-center gap-3 px-4 py-3 rounded-2xl text-xs font-extrabold text-rose-400 hover:bg-rose-950/40 hover:text-rose-300 transition-all border border-transparent hover:border-rose-900/50"
      >
        <LogOut className="w-4 h-4" />
        <span>Tizimdan chiqish</span>
      </button>
    </aside>
  );
};
