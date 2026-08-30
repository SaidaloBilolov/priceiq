import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Header } from './components/Header';
import { MobileNav } from './components/MobileNav';
import { Home } from './pages/Home';
import { ProductDetail } from './pages/ProductDetail';
import { SearchPage } from './pages/Search';
import { FavoritesPage } from './pages/Favorites';
import { AlertsPage } from './pages/Alerts';
import { ProfilePage } from './pages/Profile';
import { initTelegramApp } from './utils/telegram';
import './i18n';

export function App() {
  useEffect(() => {
    initTelegramApp();
  }, []);

  return (
    <Router>
      <div className="min-h-screen bg-gray-50 dark:bg-gray-900 text-gray-900 dark:text-white font-sans antialiased">
        <Header />
        <main className="max-w-4xl mx-auto px-4 py-4">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/product/:id" element={<ProductDetail />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="/favorites" element={<FavoritesPage />} />
            <Route path="/alerts" element={<AlertsPage />} />
            <Route path="/profile" element={<ProfilePage />} />
          </Routes>
        </main>
        <MobileNav />
      </div>
    </Router>
  );
}

export default App;
