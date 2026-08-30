import express from 'express';
import cors from 'cors';

const app = express();
const PORT = 5001;

app.use(cors());
app.use(express.json());

// Root endpoint for API status check
app.get('/', (req, res) => {
  res.json({
    status: 'online',
    message: 'PRICEIQ Bilingual REST API Server (Uzbekistan 🇺🇿)',
    version: '1.0.0',
    endpoints: {
      products: '/api/products',
      categories: '/api/categories',
      stores: '/api/stores',
      favorites: '/api/favorites',
      alerts: '/api/alerts'
    }
  });
});

// In-Memory Database initialized with rich Uzbek market data
let users: any[] = [
  {
    id: 'usr_admin',
    email: 'admin@priceiq.uz',
    password: 'password123',
    fullName: 'Admin User',
    role: 'ADMIN',
    language: 'UZ',
    createdAt: new Date().toISOString()
  },
  {
    id: 'usr_demo',
    email: 'user@priceiq.uz',
    password: 'password123',
    fullName: 'Anvar Khasanov',
    role: 'USER',
    language: 'UZ',
    createdAt: new Date().toISOString()
  }
];

let categories: any[] = [
  {
    id: 'cat_phones',
    name_uz: 'Smartfonlar va gadjetlar',
    name_ru: 'Смартфоны и гаджеты',
    description_uz: 'Zamonaviy smartfonlar, planshetlar va aqlli soatlar',
    description_ru: 'Современные смартфоны, планшеты и умные часы',
    icon: 'Smartphone'
  },
  {
    id: 'cat_laptops',
    name_uz: 'Noutbuklar va kompyuterlar',
    name_ru: 'Ноутбуки и компьютеры',
    description_uz: 'O‘qish, ish va o‘yinlar uchun noutbuklar hamda kompyuterlar',
    description_ru: 'Ноутбуки и ПК для учебы, работы и игр',
    icon: 'Laptop'
  },
  {
    id: 'cat_tv',
    name_uz: 'Televizorlar va audio',
    name_ru: 'Телевизоры и аудио',
    description_uz: '4K Smart televizorlar, akustika va uy kinoteatrlari',
    description_ru: '4K Smart телевизоры, акустика и домашние кинотеатры',
    icon: 'Tv'
  },
  {
    id: 'cat_home',
    name_uz: 'Maishiy texnika',
    name_ru: 'Бытовая техника',
    description_uz: 'Muzlatgichlar, kir yuvish mashinalari va konditsionerlar',
    description_ru: 'Холодильники, стиральные машины и кондиционеры',
    icon: 'Home'
  }
];

let stores: any[] = [
  {
    id: 'str_texnomart',
    name: 'Texnomart',
    logoUrl: 'https://images.unsplash.com/photo-1526738549149-8e07eca6c147?auto=format&fit=crop&w=120&q=80',
    websiteUrl: 'https://texnomart.uz',
    rating: 4.8
  },
  {
    id: 'str_mediapark',
    name: 'MediaPark',
    logoUrl: 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=120&q=80',
    websiteUrl: 'https://mediapark.uz',
    rating: 4.7
  },
  {
    id: 'str_asaxiy',
    name: 'Asaxiy',
    logoUrl: 'https://images.unsplash.com/photo-1550009158-9ebf69173e03?auto=format&fit=crop&w=120&q=80',
    websiteUrl: 'https://asaxiy.uz',
    rating: 4.9
  },
  {
    id: 'str_olcha',
    name: 'Olcha.uz',
    logoUrl: 'https://images.unsplash.com/photo-1512499617640-c74ae3a79d37?auto=format&fit=crop&w=120&q=80',
    websiteUrl: 'https://olcha.uz',
    rating: 4.6
  },
  {
    id: 'str_elmakon',
    name: 'Elmakon',
    logoUrl: 'https://images.unsplash.com/photo-1531297484001-80022131f5a1?auto=format&fit=crop&w=120&q=80',
    websiteUrl: 'https://elmakon.uz',
    rating: 4.5
  }
];

let products: any[] = [
  {
    id: 'prod_iphone15pro',
    name_uz: 'Apple iPhone 15 Pro Max 256GB Natural Titanium',
    name_ru: 'Apple iPhone 15 Pro Max 256GB Натуральный титан',
    description_uz: 'A17 Pro chip, titanium korpus va 48MP moslashuvchan kamera tizimi bilan eng kuchli iPhone.',
    description_ru: 'Самый мощный iPhone на чипе A17 Pro с титановым корпусом и продвинутой камерой 48 МП.',
    imageUrl: 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?auto=format&fit=crop&w=600&q=80',
    categoryId: 'cat_phones',
    lowestPrice: 14850000,
    averagePrice: 15400000,
    highestPrice: 16200000,
    storePrices: [
      {
        id: 'sp_1',
        productId: 'prod_iphone15pro',
        storeId: 'str_asaxiy',
        store: stores[2],
        price: 14850000,
        url: 'https://asaxiy.uz/product/iphone-15-pro-max',
        inStock: true,
        updatedAt: new Date().toISOString()
      },
      {
        id: 'sp_2',
        productId: 'prod_iphone15pro',
        storeId: 'str_texnomart',
        store: stores[0],
        price: 15200000,
        url: 'https://texnomart.uz/product/iphone-15-pro-max',
        inStock: true,
        updatedAt: new Date().toISOString()
      },
      {
        id: 'sp_3',
        productId: 'prod_iphone15pro',
        storeId: 'str_mediapark',
        store: stores[1],
        price: 15500000,
        url: 'https://mediapark.uz/product/iphone-15-pro-max',
        inStock: true,
        updatedAt: new Date().toISOString()
      },
      {
        id: 'sp_4',
        productId: 'prod_iphone15pro',
        storeId: 'str_olcha',
        store: stores[3],
        price: 16200000,
        url: 'https://olcha.uz/product/iphone-15-pro-max',
        inStock: false,
        updatedAt: new Date().toISOString()
      }
    ],
    priceHistory: [
      { id: 'ph_1', productId: 'prod_iphone15pro', price: 16500000, date: '2026-05-01' },
      { id: 'ph_2', productId: 'prod_iphone15pro', price: 15900000, date: '2026-06-01' },
      { id: 'ph_3', productId: 'prod_iphone15pro', price: 15400000, date: '2026-07-01' },
      { id: 'ph_4', productId: 'prod_iphone15pro', price: 14850000, date: '2026-08-01' }
    ]
  },
  {
    id: 'prod_s24ultra',
    name_uz: 'Samsung Galaxy S24 Ultra 12/512GB Titanium Black',
    name_ru: 'Samsung Galaxy S24 Ultra 12/512GB Титановый черный',
    description_uz: 'Galaxy AI sun\'iy intellekt texnologiyasi, o‘rnatilgan S Pen va 200MP kamerali flagman.',
    description_ru: 'Флагман с поддержкой технологий искусственного интеллекта Galaxy AI, встроенным S Pen и камерой 200 МП.',
    imageUrl: 'https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?auto=format&fit=crop&w=600&q=80',
    categoryId: 'cat_phones',
    lowestPrice: 13200000,
    averagePrice: 13900000,
    highestPrice: 14500000,
    storePrices: [
      {
        id: 'sp_5',
        productId: 'prod_s24ultra',
        storeId: 'str_olcha',
        store: stores[3],
        price: 13200000,
        url: 'https://olcha.uz/product/s24-ultra',
        inStock: true,
        updatedAt: new Date().toISOString()
      },
      {
        id: 'sp_6',
        productId: 'prod_s24ultra',
        storeId: 'str_texnomart',
        store: stores[0],
        price: 13800000,
        url: 'https://texnomart.uz/product/s24-ultra',
        inStock: true,
        updatedAt: new Date().toISOString()
      },
      {
        id: 'sp_7',
        productId: 'prod_s24ultra',
        storeId: 'str_elmakon',
        store: stores[4],
        price: 14500000,
        url: 'https://elmakon.uz/product/s24-ultra',
        inStock: true,
        updatedAt: new Date().toISOString()
      }
    ],
    priceHistory: [
      { id: 'ph_5', productId: 'prod_s24ultra', price: 14900000, date: '2026-05-01' },
      { id: 'ph_6', productId: 'prod_s24ultra', price: 14200000, date: '2026-06-01' },
      { id: 'ph_7', productId: 'prod_s24ultra', price: 13700000, date: '2026-07-01' },
      { id: 'ph_8', productId: 'prod_s24ultra', price: 13200000, date: '2026-08-01' }
    ]
  },
  {
    id: 'prod_macbookairm3',
    name_uz: 'Apple MacBook Air 15" M3 8/512GB Space Gray',
    name_ru: 'Apple MacBook Air 15" M3 8/512GB Космический серый',
    description_uz: 'M3 protsessorli ultra-yupqa noutbuk. 18 soatgacha avtonom ishlash muddati.',
    description_ru: 'Ультратонкий ноутбук на мощном процессоре M3. До 18 часов автономной работы.',
    imageUrl: 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=600&q=80',
    categoryId: 'cat_laptops',
    lowestPrice: 16400000,
    averagePrice: 17100000,
    highestPrice: 17900000,
    storePrices: [
      {
        id: 'sp_8',
        productId: 'prod_macbookairm3',
        storeId: 'str_texnomart',
        store: stores[0],
        price: 16400000,
        url: 'https://texnomart.uz/product/macbook-air-m3',
        inStock: true,
        updatedAt: new Date().toISOString()
      },
      {
        id: 'sp_9',
        productId: 'prod_macbookairm3',
        storeId: 'str_mediapark',
        store: stores[1],
        price: 17200000,
        url: 'https://mediapark.uz/product/macbook-air-m3',
        inStock: true,
        updatedAt: new Date().toISOString()
      }
    ],
    priceHistory: [
      { id: 'ph_9', productId: 'prod_macbookairm3', price: 17900000, date: '2026-05-01' },
      { id: 'ph_10', productId: 'prod_macbookairm3', price: 17400000, date: '2026-06-01' },
      { id: 'ph_11', productId: 'prod_macbookairm3', price: 16900000, date: '2026-07-01' },
      { id: 'ph_12', productId: 'prod_macbookairm3', price: 16400000, date: '2026-08-01' }
    ]
  },
  {
    id: 'prod_lg4ktv',
    name_uz: 'LG 55" 4K Smart OLED TV OLED55C3',
    name_ru: 'LG 55" 4K Smart OLED ТВ OLED55C3',
    description_uz: 'OLED evo displey, α9 AI Gen6 4K protsessor va Dolby Vision va Atmos qo‘llab-quvvatlovi.',
    description_ru: 'Дисплей OLED evo, процессор α9 AI Gen6 4K и поддержка Dolby Vision и Atmos.',
    imageUrl: 'https://images.unsplash.com/photo-1593784991095-a205069470b6?auto=format&fit=crop&w=600&q=80',
    categoryId: 'cat_tv',
    lowestPrice: 12900000,
    averagePrice: 13500000,
    highestPrice: 14200000,
    storePrices: [
      {
        id: 'sp_10',
        productId: 'prod_lg4ktv',
        storeId: 'str_mediapark',
        store: stores[1],
        price: 12900000,
        url: 'https://mediapark.uz/product/lg-oled-55',
        inStock: true,
        updatedAt: new Date().toISOString()
      },
      {
        id: 'sp_11',
        productId: 'prod_lg4ktv',
        storeId: 'str_asaxiy',
        store: stores[2],
        price: 13600000,
        url: 'https://asaxiy.uz/product/lg-oled-55',
        inStock: true,
        updatedAt: new Date().toISOString()
      }
    ],
    priceHistory: [
      { id: 'ph_13', productId: 'prod_lg4ktv', price: 14500000, date: '2026-05-01' },
      { id: 'ph_14', productId: 'prod_lg4ktv', price: 13900000, date: '2026-06-01' },
      { id: 'ph_15', productId: 'prod_lg4ktv', price: 13200000, date: '2026-07-01' },
      { id: 'ph_16', productId: 'prod_lg4ktv', price: 12900000, date: '2026-08-01' }
    ]
  }
];

let favorites: any[] = [
  {
    id: 'fav_1',
    userId: 'usr_demo',
    productId: 'prod_iphone15pro',
    createdAt: new Date().toISOString()
  }
];

let priceAlerts: any[] = [
  {
    id: 'alt_1',
    userId: 'usr_demo',
    productId: 'prod_iphone15pro',
    targetPrice: 14500000,
    isTriggered: false,
    createdAt: new Date().toISOString()
  }
];

// Auth Endpoints
app.post('/api/auth/login', (req, res) => {
  const { email, password } = req.body;
  const lang = req.headers['accept-language']?.startsWith('ru') ? 'ru' : 'uz';
  const user = users.find((u) => u.email === email && u.password === password);
  if (!user) {
    return res.status(401).json({
      message: lang === 'ru' ? 'Неверный email или пароль' : 'Email yoki parol noto‘g‘ri'
    });
  }
  const token = `token_${user.id}_${Date.now()}`;
  res.json({ user, token });
});

app.post('/api/auth/register', (req, res) => {
  const { fullName, email, password } = req.body;
  const lang = req.headers['accept-language']?.startsWith('ru') ? 'ru' : 'uz';

  if (!email || !password || !fullName) {
    return res.status(400).json({
      message: lang === 'ru' ? 'Заполните все обязательные поля' : 'Barcha majburiy maydonlarni to‘ldiring'
    });
  }

  const existing = users.find((u) => u.email === email);
  if (existing) {
    return res.status(400).json({
      message: lang === 'ru' ? 'Пользователь с таким email уже существует' : 'Ushbu email bilam foydalanuvchi mavjud'
    });
  }

  const newUser = {
    id: `usr_${Date.now()}`,
    email,
    password,
    fullName,
    role: 'USER',
    language: lang.toUpperCase(),
    createdAt: new Date().toISOString()
  };

  users.push(newUser);
  const token = `token_${newUser.id}_${Date.now()}`;
  res.json({ user: newUser, token });
});

// Update Profile Language
app.patch('/api/users/profile', (req, res) => {
  const authHeader = req.headers.authorization;
  const { language } = req.body;
  if (!authHeader) {
    return res.status(401).json({ message: 'Unauthorized' });
  }
  const userId = authHeader.replace('Bearer token_', '').split('_')[0];
  const user = users.find((u) => u.id === userId || authHeader.includes(u.id));

  if (user && (language === 'uz' || language === 'ru' || language === 'UZ' || language === 'RU')) {
    user.language = language.toUpperCase();
    return res.json(user);
  }
  return res.status(400).json({ message: 'Invalid language preference' });
});

// Categories CRUD
app.get('/api/categories', (req, res) => {
  const result = categories.map((cat) => {
    const count = products.filter((p) => p.categoryId === cat.id).length;
    return { ...cat, productCount: count };
  });
  res.json(result);
});

app.post('/api/categories', (req, res) => {
  const { name_uz, name_ru, description_uz, description_ru, icon } = req.body;
  const newCat = {
    id: `cat_${Date.now()}`,
    name_uz: name_uz || '',
    name_ru: name_ru || '',
    description_uz: description_uz || '',
    description_ru: description_ru || '',
    icon: icon || 'Tag'
  };
  categories.push(newCat);
  res.status(201).json(newCat);
});

app.put('/api/categories/:id', (req, res) => {
  const cat = categories.find((c) => c.id === req.params.id);
  if (!cat) return res.status(404).json({ message: 'Category not found' });

  const { name_uz, name_ru, description_uz, description_ru, icon } = req.body;
  if (name_uz !== undefined) cat.name_uz = name_uz;
  if (name_ru !== undefined) cat.name_ru = name_ru;
  if (description_uz !== undefined) cat.description_uz = description_uz;
  if (description_ru !== undefined) cat.description_ru = description_ru;
  if (icon !== undefined) cat.icon = icon;

  res.json(cat);
});

app.delete('/api/categories/:id', (req, res) => {
  categories = categories.filter((c) => c.id !== req.params.id);
  res.json({ success: true });
});

// Stores CRUD
app.get('/api/stores', (req, res) => {
  res.json(stores);
});

app.post('/api/stores', (req, res) => {
  const { name, logoUrl, websiteUrl, rating } = req.body;
  const newStore = {
    id: `str_${Date.now()}`,
    name,
    logoUrl: logoUrl || 'https://images.unsplash.com/photo-1526738549149-8e07eca6c147?auto=format&fit=crop&w=120&q=80',
    websiteUrl: websiteUrl || '#',
    rating: rating || 4.5
  };
  stores.push(newStore);
  res.status(201).json(newStore);
});

// Products CRUD
app.get('/api/products', (req, res) => {
  const { search, categoryId, sort } = req.query;
  let result = [...products];

  if (categoryId) {
    result = result.filter((p) => p.categoryId === categoryId);
  }

  if (search) {
    const q = (search as string).toLowerCase();
    result = result.filter(
      (p) =>
        p.name_uz.toLowerCase().includes(q) ||
        p.name_ru.toLowerCase().includes(q) ||
        p.description_uz.toLowerCase().includes(q) ||
        p.description_ru.toLowerCase().includes(q)
    );
  }

  result = result.map((p) => {
    const category = categories.find((c) => c.id === p.categoryId);
    return { ...p, category };
  });

  res.json(result);
});

app.get('/api/products/:id', (req, res) => {
  const p = products.find((prod) => prod.id === req.params.id);
  if (!p) return res.status(404).json({ message: 'Product not found' });
  const category = categories.find((c) => c.id === p.categoryId);
  res.json({ ...p, category });
});

app.post('/api/products', (req, res) => {
  const { name_uz, name_ru, description_uz, description_ru, imageUrl, categoryId, lowestPrice, storePrices } = req.body;

  const newProd = {
    id: `prod_${Date.now()}`,
    name_uz: name_uz || '',
    name_ru: name_ru || '',
    description_uz: description_uz || '',
    description_ru: description_ru || '',
    imageUrl: imageUrl || 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=600&q=80',
    categoryId,
    lowestPrice: Number(lowestPrice) || 1000000,
    averagePrice: Math.round(Number(lowestPrice) * 1.05) || 1050000,
    highestPrice: Math.round(Number(lowestPrice) * 1.12) || 1120000,
    storePrices: storePrices || [
      {
        id: `sp_${Date.now()}`,
        storeId: stores[0].id,
        store: stores[0],
        price: Number(lowestPrice) || 1000000,
        url: stores[0].websiteUrl,
        inStock: true,
        updatedAt: new Date().toISOString()
      }
    ],
    priceHistory: [
      { id: `ph_${Date.now()}`, price: Number(lowestPrice) || 1000000, date: new Date().toISOString().split('T')[0] }
    ]
  };

  products.push(newProd);
  res.status(201).json(newProd);
});

app.put('/api/products/:id', (req, res) => {
  const p = products.find((prod) => prod.id === req.params.id);
  if (!p) return res.status(404).json({ message: 'Product not found' });

  const { name_uz, name_ru, description_uz, description_ru, imageUrl, categoryId, lowestPrice } = req.body;
  if (name_uz !== undefined) p.name_uz = name_uz;
  if (name_ru !== undefined) p.name_ru = name_ru;
  if (description_uz !== undefined) p.description_uz = description_uz;
  if (description_ru !== undefined) p.description_ru = description_ru;
  if (imageUrl !== undefined) p.imageUrl = imageUrl;
  if (categoryId !== undefined) p.categoryId = categoryId;
  if (lowestPrice !== undefined) p.lowestPrice = Number(lowestPrice);

  res.json(p);
});

app.delete('/api/products/:id', (req, res) => {
  products = products.filter((p) => p.id !== req.params.id);
  res.json({ success: true });
});

// Favorites
app.get('/api/favorites', (req, res) => {
  const result = favorites.map((fav) => {
    const product = products.find((p) => p.id === fav.productId);
    const category = categories.find((c) => c.id === product?.categoryId);
    return { ...fav, product: { ...product, category } };
  });
  res.json(result);
});

app.post('/api/favorites', (req, res) => {
  const { productId } = req.body;
  const authHeader = req.headers.authorization || '';
  const userId = authHeader.replace('Bearer token_', '').split('_')[0] || 'usr_demo';

  const existing = favorites.find((f) => f.productId === productId && f.userId === userId);
  if (existing) return res.json(existing);

  const newFav = {
    id: `fav_${Date.now()}`,
    userId,
    productId,
    createdAt: new Date().toISOString()
  };
  favorites.push(newFav);
  res.status(201).json(newFav);
});

app.delete('/api/favorites/:productId', (req, res) => {
  const authHeader = req.headers.authorization || '';
  const userId = authHeader.replace('Bearer token_', '').split('_')[0] || 'usr_demo';
  favorites = favorites.filter((f) => !(f.productId === req.params.productId && f.userId === userId));
  res.json({ success: true });
});

// Price Alerts
app.get('/api/alerts', (req, res) => {
  const result = priceAlerts.map((alt) => {
    const product = products.find((p) => p.id === alt.productId);
    const category = categories.find((c) => c.id === product?.categoryId);
    return {
      ...alt,
      currentLowestPrice: product?.lowestPrice || 0,
      isTriggered: (product?.lowestPrice || 0) <= alt.targetPrice,
      product: { ...product, category }
    };
  });
  res.json(result);
});

app.post('/api/alerts', (req, res) => {
  const { productId, targetPrice } = req.body;
  const authHeader = req.headers.authorization || '';
  const userId = authHeader.replace('Bearer token_', '').split('_')[0] || 'usr_demo';

  const newAlert = {
    id: `alt_${Date.now()}`,
    userId,
    productId,
    targetPrice: Number(targetPrice),
    isTriggered: false,
    createdAt: new Date().toISOString()
  };
  priceAlerts.push(newAlert);
  res.status(201).json(newAlert);
});

app.delete('/api/alerts/:id', (req, res) => {
  priceAlerts = priceAlerts.filter((a) => a.id !== req.params.id);
  res.json({ success: true });
});

app.listen(PORT, () => {
  console.log(`PRICEIQ Bilingual API Server running on port ${PORT}`);
});
