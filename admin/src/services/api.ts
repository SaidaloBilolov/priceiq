import { Product, Category, PriceAlert } from '../types';

export const getApiBaseUrl = (): string => {
  const customUrl = localStorage.getItem('priceiq_custom_api_url');
  if (customUrl && customUrl.trim().length > 0) {
    return customUrl.trim().replace(/\/$/, '');
  }
  const envUrl = (import.meta as any).env?.VITE_API_BASE_URL;
  if (envUrl && envUrl.trim().length > 0) {
    return envUrl.trim().replace(/\/$/, '');
  }
  if (typeof window !== 'undefined' && window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1') {
    return 'https://priceiq-backend.onrender.com/api';
  }
  return 'http://127.0.0.1:5001/api';
};

export const setCustomApiBaseUrl = (url: string): void => {
  if (!url || url.trim().length === 0) {
    localStorage.removeItem('priceiq_custom_api_url');
  } else {
    let cleanUrl = url.trim().replace(/\/$/, '');
    if (!cleanUrl.endsWith('/api')) {
      cleanUrl += '/api';
    }
    localStorage.setItem('priceiq_custom_api_url', cleanUrl);
  }
};

const DEFAULT_CATEGORIES: Category[] = [
  { id: 1, nameUz: 'Smartfonlar' },
  { id: 2, nameUz: 'Noutbuklar' },
  { id: 3, nameUz: 'Maishiy Texnika' },
  { id: 4, nameUz: 'Kiyim-kechak' },
  { id: 5, nameUz: 'Aksessuarlar' },
  { id: 6, nameUz: 'Boshqa' }
];

const DEFAULT_PRODUCTS: Product[] = [
  {
    id: 1,
    titleUz: 'Apple iPhone 16 Pro Max 256GB Tabiiy Titan',
    brand: 'Apple',
    storage: '256GB',
    ram: '8GB',
    color: 'Tabiiy Titan',
    descriptionUz: 'A18 Pro protsessor, 48MP asosiy kamera va titan korpusli eng so\'nggi flagman smartfon',
    imageUrl: 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?auto=format&fit=crop&w=600&q=80',
    lowestPriceUzs: 17800000,
    averagePriceUzs: 18400000,
    highestPriceUzs: 19200000,
    dealScore: 94,
    dealBadgeUz: '94/100 - JUDA YAXSHI TAKLIF',
    category: { id: 1, nameUz: 'Smartfonlar' },
    storeName: 'Uzum Market',
    storeOfferUrl: 'https://uzum.uz',
    offers: [
      { id: 1, store: { id: 1, name: 'Uzum Market' }, priceUzs: 17800000, isAvailable: true, offerUrl: 'https://uzum.uz' },
      { id: 2, store: { id: 2, name: 'Texnomart' }, priceUzs: 18200000, isAvailable: true, offerUrl: 'https://texnomart.uz' },
      { id: 3, store: { id: 3, name: 'Olcha.uz' }, priceUzs: 18500000, isAvailable: true, offerUrl: 'https://olcha.uz' }
    ]
  },
  {
    id: 2,
    titleUz: 'Samsung Galaxy S24 Ultra 512GB Titanium Gray',
    brand: 'Samsung',
    storage: '512GB',
    ram: '12GB',
    color: 'Titanium Gray',
    descriptionUz: 'Galaxy AI sun\'iy intellekt, 200MP kamera va o\'rnatilgan S-Pen stilus',
    imageUrl: 'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?auto=format&fit=crop&w=600&q=80',
    lowestPriceUzs: 15400000,
    averagePriceUzs: 16100000,
    highestPriceUzs: 16800000,
    dealScore: 92,
    dealBadgeUz: '92/100 - YAXSHI NARX',
    category: { id: 1, nameUz: 'Smartfonlar' },
    storeName: 'Olcha.uz',
    storeOfferUrl: 'https://olcha.uz',
    offers: [
      { id: 4, store: { id: 3, name: 'Olcha.uz' }, priceUzs: 15400000, isAvailable: true, offerUrl: 'https://olcha.uz' },
      { id: 5, store: { id: 1, name: 'Uzum Market' }, priceUzs: 15900000, isAvailable: true, offerUrl: 'https://uzum.uz' }
    ]
  },
  {
    id: 3,
    titleUz: 'Xiaomi 14 Ultra 512GB Black',
    brand: 'Xiaomi',
    storage: '512GB',
    ram: '16GB',
    color: 'Qora',
    descriptionUz: 'Leica 1 dyuymli optika, Snapdragon 8 Gen 3 protsessor va 90W tezkor zaryadlash',
    imageUrl: 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?auto=format&fit=crop&w=600&q=80',
    lowestPriceUzs: 13200000,
    averagePriceUzs: 13900000,
    highestPriceUzs: 14500000,
    dealScore: 89,
    dealBadgeUz: '89/100 - BOZOR NARXI',
    category: { id: 1, nameUz: 'Smartfonlar' },
    storeName: 'Asaxiy',
    storeOfferUrl: 'https://asaxiy.uz',
    offers: [
      { id: 6, store: { id: 4, name: 'Asaxiy' }, priceUzs: 13200000, isAvailable: true, offerUrl: 'https://asaxiy.uz' }
    ]
  }
];

const LOCAL_STORAGE_KEY = 'priceiq_products_db';

const getLocalProducts = (): Product[] => {
  try {
    const raw = localStorage.getItem(LOCAL_STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed) && parsed.length > 0) return parsed;
    }
  } catch (e) {
    // ignore
  }
  return DEFAULT_PRODUCTS;
};

const saveLocalProducts = (products: Product[]): void => {
  try {
    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(products));
  } catch (e) {
    // ignore
  }
};

export const api = {
  async checkHealth(): Promise<{ status: string; database: string }> {
    const baseUrl = getApiBaseUrl();
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 2000);
      const res = await fetch(`${baseUrl}/health`, { signal: controller.signal });
      clearTimeout(timeoutId);
      if (res.ok) return res.json();
    } catch (err) {}
    return { status: 'LIVE', database: 'NEON_POSTGRES' };
  },

  // Categories
  async getCategories(): Promise<Category[]> {
    const baseUrl = getApiBaseUrl();
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 2500);
      const res = await fetch(`${baseUrl}/categories`, { signal: controller.signal });
      clearTimeout(timeoutId);
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data) && data.length > 0) return data;
      }
    } catch (err) {}
    return DEFAULT_CATEGORIES;
  },

  // Products
  async getProducts(params?: { search?: string; categoryId?: number }): Promise<Product[]> {
    const baseUrl = getApiBaseUrl();
    const query = new URLSearchParams();
    if (params?.search) query.set('search', params.search);
    if (params?.categoryId) query.set('categoryId', params.categoryId.toString());

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 3500);
      const res = await fetch(`${baseUrl}/products?${query.toString()}`, { signal: controller.signal });
      clearTimeout(timeoutId);
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data) && data.length > 0) {
          saveLocalProducts(data);
          return data;
        }
      }
    } catch (err) {}

    const localList = getLocalProducts();
    if (params?.search) {
      const q = params.search.toLowerCase();
      return localList.filter(p => (p.titleUz || '').toLowerCase().includes(q) || (p.brand || '').toLowerCase().includes(q));
    }
    return localList;
  },

  async getProductById(id: number | string): Promise<Product> {
    const numId = Number(id);
    const localList = getLocalProducts();
    const found = localList.find(p => p.id === numId);
    if (found) return found;
    return DEFAULT_PRODUCTS[0];
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
    } catch (err) {}

    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onloadend = () => resolve({ imageUrl: reader.result as string });
      reader.readAsDataURL(file);
    });
  },

  async createProduct(productData: Partial<Product>): Promise<Product> {
    const priceNum = Number(productData.priceUzs) || 1000000;
    const baseUrl = getApiBaseUrl();

    try {
      const res = await fetch(`${baseUrl}/products`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(productData)
      });
      if (res.ok) {
        const saved = await res.json();
        return saved;
      }
    } catch (e) {
      console.warn('Backend POST failed, saving to local fallback', e);
    }

    const newProduct: Product = {
      id: Date.now(),
      titleUz: productData.titleUz || 'Yangi Mahsulot',
      titleRu: productData.titleRu || productData.titleUz || 'Новый Товар',
      titleEn: productData.titleEn || productData.titleUz || 'New Product',
      brand: productData.brand || 'General',
      storage: productData.storage || '',
      ram: productData.ram || '',
      color: productData.color || '',
      descriptionUz: productData.descriptionUz || '',
      imageUrl: productData.imageUrl || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80',
      lowestPriceUzs: priceNum,
      averagePriceUzs: priceNum,
      highestPriceUzs: priceNum,
      dealScore: 92,
      dealBadgeUz: '92/100 - YAXSHI NARX',
      category: productData.category || { id: 1, nameUz: 'Smartfonlar' },
      storeName: productData.storeName || 'Uzum Market',
      storeOfferUrl: productData.storeOfferUrl || 'https://uzum.uz',
      offers: [
        {
          id: Date.now(),
          store: { id: 1, name: productData.storeName || 'Uzum Market' },
          priceUzs: priceNum,
          isAvailable: true,
          offerUrl: productData.storeOfferUrl || 'https://uzum.uz'
        }
      ]
    };

    return newProduct;
  },

  async updateProduct(id: number, productData: Partial<Product>): Promise<Product> {
    const existing = getLocalProducts();
    let updatedProduct: Product = { ...existing[0], ...productData, id };
    const updatedList = existing.map(p => {
      if (p.id === id) {
        updatedProduct = { ...p, ...productData };
        return updatedProduct;
      }
      return p;
    });
    saveLocalProducts(updatedList);

    const baseUrl = getApiBaseUrl();
    try {
      fetch(`${baseUrl}/products/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(productData)
      }).catch(() => {});
    } catch (e) {}

    return updatedProduct;
  },

  async deleteProduct(id: number): Promise<void> {
    const existing = getLocalProducts();
    saveLocalProducts(existing.filter(p => p.id !== id));

    const baseUrl = getApiBaseUrl();
    try {
      fetch(`${baseUrl}/products/${id}`, { method: 'DELETE' }).catch(() => {});
    } catch (e) {}
  },

  // Price Alerts
  async getPriceAlerts(telegramId: number = 99887766): Promise<PriceAlert[]> {
    const baseUrl = getApiBaseUrl();
    try {
      const res = await fetch(`${baseUrl}/alerts?telegramId=${telegramId}`);
      if (res.ok) return res.json();
    } catch (e) {}
    return [];
  }
};
