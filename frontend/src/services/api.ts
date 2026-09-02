import { Product, Category, Favorite, PriceAlert, User } from '../types';

export const getApiBaseUrl = (): string => {
  const customUrl = localStorage.getItem('priceiq_custom_api_url');
  if (customUrl && customUrl.trim().length > 0) {
    return customUrl.trim().replace(/\/$/, '');
  }
  const envUrl = (import.meta as any).env?.VITE_API_BASE_URL;
  if (envUrl && envUrl.trim().length > 0) {
    return envUrl.trim().replace(/\/$/, '');
  }
  return 'https://priceiq-backend.onrender.com/api';
};

const API_BASE_URL = getApiBaseUrl();

const DEFAULT_CATEGORIES: Category[] = [
  { id: 1, nameUz: 'Smartfonlar va Gadjetlar', nameRu: 'Смартфоны и гаджеты', nameEn: 'Smartphones & Gadgets' },
  { id: 2, nameUz: 'Noutbuklar va Kompyuterlar', nameRu: 'Ноутбуки и ПК', nameEn: 'Laptops & Computers' },
  { id: 3, nameUz: 'Televizor va Audio', nameRu: 'ТВ и Аудио', nameEn: 'TV & Audio' },
  { id: 4, nameUz: 'Maishiy Texnika', nameRu: 'Бытовая техника', nameEn: 'Appliances' },
  { id: 5, nameUz: 'Kiyim va Poyabzal', nameRu: 'Одежда и обувь', nameEn: 'Clothing & Shoes' },
  { id: 6, nameUz: "Go'zallik va Parvarish", nameRu: 'Красота и уход', nameEn: 'Beauty & Care' },
  { id: 7, nameUz: 'Uy va Oshxona', nameRu: 'Дом и кухня', nameEn: 'Home & Kitchen' },
  { id: 8, nameUz: 'Avtotovarlar', nameRu: 'Автотовары', nameEn: 'Auto goods' },
  { id: 9, nameUz: 'Sport va Salomatlik', nameRu: 'Спорт и здоровье', nameEn: 'Sport & Health' },
  { id: 10, nameUz: 'Bolalar Mahsulotlari', nameRu: 'Детские товары', nameEn: 'Kids & Baby' },
  { id: 11, nameUz: 'Kitoblar va Kanselyariya', nameRu: 'Книги и канцелярия', nameEn: 'Books & Stationery' },
  { id: 12, nameUz: 'Boshqa Mahsulotlar', nameRu: 'Другие товары', nameEn: 'Other Products' }
];

const DEFAULT_PRODUCTS: Product[] = [];

export const api = {
  // User Profile & Phone Number Management
  async getUserProfile(telegramId: number, firstName?: string, username?: string, languageCode?: string): Promise<User> {
    const baseUrl = getApiBaseUrl();
    try {
      const params = new URLSearchParams({ telegramId: telegramId.toString() });
      if (firstName) params.set('firstName', firstName);
      if (username) params.set('username', username);
      if (languageCode) params.set('languageCode', languageCode);

      const res = await fetch(`${baseUrl}/users/me?${params.toString()}`);
      if (res.ok) return res.json();
    } catch (e) {}
    return {
      id: 1,
      telegramId,
      firstName: firstName || 'Foydalanuvchi',
      username: username || 'user',
      languageCode: languageCode || 'uz',
      phoneNumber: '+998901234567'
    };
  },

  async updatePhoneNumber(telegramId: number, phoneNumber: string, languageCode?: string): Promise<User> {
    const baseUrl = getApiBaseUrl();
    try {
      const res = await fetch(`${baseUrl}/users/phone`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ telegramId, phoneNumber, languageCode })
      });
      if (res.ok) return res.json();
    } catch (e) {}
    return {
      id: 1,
      telegramId,
      firstName: 'Foydalanuvchi',
      phoneNumber,
      languageCode: languageCode || 'uz'
    };
  },

  // Categories
  async getCategories(): Promise<Category[]> {
    const baseUrl = getApiBaseUrl();
    try {
      const res = await fetch(`${baseUrl}/categories`);
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data) && data.length > 0) return data;
      }
    } catch (e) {}
    return DEFAULT_CATEGORIES;
  },

  // Products
  async getProducts(params?: { search?: string; categoryId?: number }): Promise<Product[]> {
    const baseUrl = getApiBaseUrl();

    // 1. If search query provided, search live Uzum Market products first with no-store cache
    if (params?.search && params.search.trim().length > 0) {
      try {
        const rootUrl = baseUrl.replace(/\/api$/, '');
        const res = await fetch(`${rootUrl}/api/v1/products/search?query=${encodeURIComponent(params.search.trim())}&t=${Date.now()}`, {
          cache: 'no-store',
          headers: { 'Cache-Control': 'no-cache, no-store, must-revalidate' }
        });
        if (res.ok) {
          const uzumItems = await res.json();
          if (Array.isArray(uzumItems) && uzumItems.length > 0) {
            return uzumItems.map((u: any, idx: number) => ({
              id: u.productId || (idx + 1000),
              titleUz: u.title,
              titleRu: u.title,
              titleEn: u.title,
              imageUrl: u.mainImage || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80',
              lowestPriceUzs: u.price || 0,
              averagePriceUzs: u.fullPrice || u.price || 0,
              highestPriceUzs: u.fullPrice || u.price || 0,
              dealScore: 90,
              dealBadgeUz: `⭐ ${u.rating || 4.8} - UZUM MARKET`,
              dealBadgeRu: `⭐ ${u.rating || 4.8} - UZUM MARKET`,
              dealBadgeEn: `⭐ ${u.rating || 4.8} - UZUM MARKET`,
              category: { id: 1, nameUz: 'Uzum Market', nameRu: 'Uzum Market', nameEn: 'Uzum Market' },
              storeName: 'Uzum Market',
              storeOfferUrl: u.productUrl || `https://uzum.uz/product/${u.productId}`,
              offers: [
                {
                  id: u.productId || (idx + 1000),
                  store: { id: 1, name: 'Uzum Market' },
                  priceUzs: u.price || 0,
                  isAvailable: true,
                  offerUrl: u.productUrl || `https://uzum.uz/product/${u.productId}`
                }
              ]
            }));
          }
        }
      } catch (e) {
        console.warn('Live Uzum search fetch error', e);
      }

      // If user performed a search and zero results came from live API, check DB with no-store
      try {
        const query = new URLSearchParams({ search: params.search.trim(), t: Date.now().toString() });
        const res = await fetch(`${baseUrl}/products?${query.toString()}`, { cache: 'no-store' });
        if (res.ok) {
          const data = await res.json();
          if (Array.isArray(data)) {
            return data;
          }
        }
      } catch (e) {}

      return [];
    }

    // 2. Standard home page catalog query (without search param)
    const query = new URLSearchParams({ t: Date.now().toString() });
    if (params?.categoryId) query.set('categoryId', params.categoryId.toString());

    try {
      const res = await fetch(`${baseUrl}/products?${query.toString()}`, { cache: 'no-store' });
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data) && data.length > 0) {
          return data;
        }
      }
    } catch (e) {
      console.warn('Backend fetch fallback', e);
    }
    return DEFAULT_PRODUCTS;
  },

  async getProductById(id: number | string): Promise<Product> {
    const baseUrl = getApiBaseUrl();
    try {
      const res = await fetch(`${baseUrl}/products/${id}`);
      if (res.ok) return res.json();
    } catch (e) {}
    return DEFAULT_PRODUCTS[0];
  },

  // Admin Product CRUD APIs
  async createProduct(productData: Partial<Product>): Promise<Product> {
    const baseUrl = getApiBaseUrl();
    try {
      const res = await fetch(`${baseUrl}/products`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(productData)
      });
      if (res.ok) return res.json();
    } catch (e) {}

    return DEFAULT_PRODUCTS[0];
  },

  async updateProduct(id: number, productData: Partial<Product>): Promise<Product> {
    const baseUrl = getApiBaseUrl();
    try {
      const res = await fetch(`${baseUrl}/products/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(productData)
      });
      if (res.ok) return res.json();
    } catch (e) {}

    return DEFAULT_PRODUCTS[0];
  },

  async deleteProduct(id: number): Promise<void> {
    const baseUrl = getApiBaseUrl();
    try {
      await fetch(`${baseUrl}/products/${id}`, { method: 'DELETE' });
    } catch (e) {}
  },

  // Image Upload API
  async uploadProductImage(file: File): Promise<{ imageUrl: string }> {
    const baseUrl = getApiBaseUrl();
    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await fetch(`${baseUrl}/products/upload-image`, {
        method: 'POST',
        body: formData
      });
      if (res.ok) return res.json();
    } catch (e) {}

    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onloadend = () => resolve({ imageUrl: reader.result as string });
      reader.readAsDataURL(file);
    });
  },

  // Favorites
  async getFavorites(telegramId: number = 99887766): Promise<Product[]> {
    const baseUrl = getApiBaseUrl();
    try {
      const res = await fetch(`${baseUrl}/favorites?telegramId=${telegramId}`);
      if (res.ok) {
        const favs: Favorite[] = await res.json();
        return favs.map(f => f.product);
      }
    } catch (e) {}
    return [DEFAULT_PRODUCTS[0]];
  },

  async toggleFavorite(telegramId: number, productId: number): Promise<{ favorited: boolean }> {
    const baseUrl = getApiBaseUrl();
    try {
      const res = await fetch(`${baseUrl}/favorites/toggle`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ telegramId, productId })
      });
      if (res.ok) return { favorited: true };
    } catch (e) {}
    return { favorited: true };
  },

  // Price Alerts
  async getPriceAlerts(telegramId: number = 99887766): Promise<PriceAlert[]> {
    const baseUrl = getApiBaseUrl();
    try {
      const res = await fetch(`${baseUrl}/alerts?telegramId=${telegramId}`);
      if (res.ok) return res.json();
    } catch (e) {}
    return [];
  },

  async createPriceAlert(telegramId: number, productId: number, targetPriceUzs: number): Promise<PriceAlert> {
    const baseUrl = getApiBaseUrl();
    try {
      const res = await fetch(`${baseUrl}/alerts`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ telegramId, productId, targetPriceUzs })
      });
      if (res.ok) return res.json();
    } catch (e) {}
    return {
      id: Date.now(),
      userId: 1,
      product: DEFAULT_PRODUCTS[0],
      targetPriceUzs,
      isActive: true,
      createdAt: new Date().toISOString()
    };
  },

  async deletePriceAlert(id: number): Promise<void> {
    const baseUrl = getApiBaseUrl();
    try {
      fetch(`${baseUrl}/alerts/${id}`, { method: 'DELETE' }).catch(() => {});
    } catch (e) {}
  }
};
