import { Product, Category, Favorite, PriceAlert, User } from '../types';

const API_BASE_URL = (import.meta as any).env?.VITE_API_BASE_URL || 'http://localhost:5001/api';

const DEFAULT_CATEGORIES: Category[] = [
  { id: 1, nameUz: 'Smartfonlar', nameRu: 'Смартфоны', nameEn: 'Smartphones' },
  { id: 2, nameUz: 'Noutbuklar', nameRu: 'Ноутбуки', nameEn: 'Laptops' },
  { id: 3, nameUz: 'Maishiy Texnika', nameRu: 'Бытовая техника', nameEn: 'Appliances' },
  { id: 4, nameUz: 'Kiyim-kechak', nameRu: 'Одежда', nameEn: 'Clothing' },
  { id: 5, nameUz: 'Aksessuarlar', nameRu: 'Аксессуары', nameEn: 'Accessories' },
  { id: 6, nameUz: 'Boshqa', nameRu: 'Другое', nameEn: 'Other' }
];

const DEFAULT_PRODUCTS: Product[] = [
  {
    id: 1,
    titleUz: 'Apple iPhone 16 Pro Max 256GB Tabiiy Titan',
    titleRu: 'Apple iPhone 16 Pro Max 256GB Натуральный Титан',
    titleEn: 'Apple iPhone 16 Pro Max 256GB Natural Titanium',
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
    dealBadgeRu: '94/100 - ОТЛИЧНОЕ ПРЕДЛОЖЕНИЕ',
    dealBadgeEn: '94/100 - GREAT DEAL',
    category: { id: 1, nameUz: 'Smartfonlar', nameRu: 'Смартфоны', nameEn: 'Smartphones' },
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
    titleRu: 'Samsung Galaxy S24 Ultra 512GB Titanium Gray',
    titleEn: 'Samsung Galaxy S24 Ultra 512GB Titanium Gray',
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
    dealBadgeRu: '92/100 - ХОРОШАЯ ЦЕНА',
    dealBadgeEn: '92/100 - GOOD DEAL',
    category: { id: 1, nameUz: 'Smartfonlar', nameRu: 'Смартфоны', nameEn: 'Smartphones' },
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
    titleRu: 'Xiaomi 14 Ultra 512GB Черный',
    titleEn: 'Xiaomi 14 Ultra 512GB Black',
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
    dealBadgeRu: '89/100 - РЫНОЧНАЯ ЦЕНА',
    dealBadgeEn: '89/100 - FAIR DEAL',
    category: { id: 1, nameUz: 'Smartfonlar', nameRu: 'Смартфоны', nameEn: 'Smartphones' },
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

const saveLocalProducts = (prods: Product[]): void => {
  try {
    localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(prods));
  } catch (e) {
    // ignore
  }
};

export const api = {
  // User Profile & Phone Number Management
  async getUserProfile(telegramId: number, firstName?: string, username?: string, languageCode?: string): Promise<User> {
    try {
      const params = new URLSearchParams({ telegramId: telegramId.toString() });
      if (firstName) params.set('firstName', firstName);
      if (username) params.set('username', username);
      if (languageCode) params.set('languageCode', languageCode);

      const res = await fetch(`${API_BASE_URL}/users/me?${params.toString()}`);
      if (res.ok) return res.json();
    } catch (e) {
      // ignore
    }
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
    try {
      const res = await fetch(`${API_BASE_URL}/users/phone`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ telegramId, phoneNumber, languageCode })
      });
      if (res.ok) return res.json();
    } catch (e) {
      // ignore
    }
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
    try {
      const res = await fetch(`${API_BASE_URL}/categories`);
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data) && data.length > 0) return data;
      }
    } catch (e) {
      // ignore
    }
    return DEFAULT_CATEGORIES;
  },

  // Products
  async getProducts(params?: { search?: string; categoryId?: number }): Promise<Product[]> {
    const query = new URLSearchParams();
    if (params?.search) query.set('search', params.search);
    if (params?.categoryId) query.set('categoryId', params.categoryId.toString());

    try {
      const res = await fetch(`${API_BASE_URL}/products?${query.toString()}`);
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data) && data.length > 0) return data;
      }
    } catch (e) {
      // ignore
    }

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

  // Admin Product CRUD APIs
  async createProduct(productData: Partial<Product>): Promise<Product> {
    const priceNum = Number(productData.priceUzs) || 1000000;
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
      descriptionRu: productData.descriptionRu || '',
      descriptionEn: productData.descriptionEn || '',
      imageUrl: productData.imageUrl || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80',
      lowestPriceUzs: priceNum,
      averagePriceUzs: priceNum,
      highestPriceUzs: priceNum,
      dealScore: 92,
      dealBadgeUz: '92/100 - YAXSHI NARX',
      dealBadgeRu: '92/100 - ХОРОШАЯ ЦЕНА',
      dealBadgeEn: '92/100 - GOOD DEAL',
      category: productData.category || { id: 1, nameUz: 'Smartfonlar', nameRu: 'Смартфоны', nameEn: 'Smartphones' },
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

    const existing = getLocalProducts();
    saveLocalProducts([newProduct, ...existing]);

    try {
      fetch(`${API_BASE_URL}/products`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(productData)
      }).catch(() => {});
    } catch (e) {}

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

    try {
      fetch(`${API_BASE_URL}/products/${id}`, {
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
    try {
      fetch(`${API_BASE_URL}/products/${id}`, { method: 'DELETE' }).catch(() => {});
    } catch (e) {}
  },

  // Image Upload API
  async uploadProductImage(file: File): Promise<{ imageUrl: string }> {
    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await fetch(`${API_BASE_URL}/products/upload-image`, {
        method: 'POST',
        body: formData
      });
      if (res.ok) return res.json();
    } catch (e) {
      // ignore
    }
    return new Promise((resolve) => {
      const reader = new FileReader();
      reader.onloadend = () => resolve({ imageUrl: reader.result as string });
      reader.readAsDataURL(file);
    });
  },

  // Favorites
  async getFavorites(telegramId: number = 99887766): Promise<Product[]> {
    try {
      const res = await fetch(`${API_BASE_URL}/favorites?telegramId=${telegramId}`);
      if (res.ok) {
        const favs: Favorite[] = await res.json();
        return favs.map(f => f.product);
      }
    } catch (e) {
      // ignore
    }
    return [DEFAULT_PRODUCTS[0]];
  },

  async toggleFavorite(telegramId: number, productId: number): Promise<{ favorited: boolean }> {
    try {
      const res = await fetch(`${API_BASE_URL}/favorites/toggle`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ telegramId, productId })
      });
      if (res.ok) return { favorited: true };
    } catch (e) {
      // ignore
    }
    return { favorited: true };
  },

  // Price Alerts
  async getPriceAlerts(telegramId: number = 99887766): Promise<PriceAlert[]> {
    try {
      const res = await fetch(`${API_BASE_URL}/alerts?telegramId=${telegramId}`);
      if (res.ok) return res.json();
    } catch (e) {
      // ignore
    }
    return [];
  },

  async createPriceAlert(telegramId: number, productId: number, targetPriceUzs: number): Promise<PriceAlert> {
    try {
      const res = await fetch(`${API_BASE_URL}/alerts`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ telegramId, productId, targetPriceUzs })
      });
      if (res.ok) return res.json();
    } catch (e) {
      // ignore
    }
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
    try {
      fetch(`${API_BASE_URL}/alerts/${id}`, { method: 'DELETE' }).catch(() => {});
    } catch (e) {}
  }
};
