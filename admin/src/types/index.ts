export interface Category {
  id: number;
  nameUz: string;
  nameRu?: string;
  nameEn?: string;
  icon?: string;
}

export interface Store {
  id: number;
  name: string;
  logoUrl?: string;
  websiteUrl?: string;
  rating?: number;
}

export interface ProductOffer {
  id: number;
  store: Store;
  priceUzs: number;
  oldPriceUzs?: number;
  isAvailable: boolean;
  offerUrl: string;
  updatedAt?: string;
}

export interface PriceHistoryPoint {
  id: number;
  priceUzs: number;
  recordedAt: string;
}

export interface Product {
  id: number;
  titleUz: string;
  titleRu?: string;
  titleEn?: string;
  descriptionUz?: string;
  descriptionRu?: string;
  descriptionEn?: string;
  brand?: string;
  model?: string;
  storage?: string;
  ram?: string;
  color?: string;
  imageUrl: string;
  categoryId?: number;
  category?: Category;
  storeName?: string;
  storeOfferUrl?: string;
  priceUzs?: number;
  lowestPriceUzs: number;
  averagePriceUzs: number;
  highestPriceUzs: number;
  dealScore: number;
  dealBadgeUz: string;
  offers: ProductOffer[];
  priceHistory?: PriceHistoryPoint[];
}

export interface PriceAlert {
  id: number;
  userId: number;
  product: Product;
  targetPriceUzs: number;
  isActive: boolean;
  createdAt: string;
}
