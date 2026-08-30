import { neon } from '@neondatabase/serverless';

const sql = neon('postgresql://neondb_owner:npg_XYvxkjzQ0nc2@ep-frosty-flower-ayl6bzxy-pooler.c-5.us-east-2.aws.neon.tech/neondb?sslmode=require');

export default async function handler(req, res) {
  // Set CORS headers
  res.setHeader('Access-Control-Allow-Credentials', 'true');
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS,PATCH,DELETE,POST,PUT');
  res.setHeader(
    'Access-Control-Allow-Headers',
    'X-CSRF-Token, X-Requested-With, Accept, Accept-Version, Content-Length, Content-MD5, Content-Type, Date, X-Api-Version'
  );

  if (req.method === 'OPTIONS') {
    res.status(200).end();
    return;
  }

  try {
    if (req.method === 'GET') {
      const { id, search, categoryId } = req.query;

      if (id) {
        const rows = await sql`
          SELECT p.*, c.name_uz as cat_name_uz, c.name_ru as cat_name_ru, c.name_en as cat_name_en
          FROM products p
          LEFT JOIN categories c ON p.category_id = c.id
          WHERE p.id = ${id}
        `;
        if (rows.length === 0) return res.status(404).json({ error: 'Product not found' });
        
        const p = rows[0];
        const offers = await sql`
          SELECT po.*, s.name as store_name, s.logo_url as store_logo
          FROM product_offers po
          LEFT JOIN stores s ON po.store_id = s.id
          WHERE po.product_id = ${p.id}
        `;

        const lowestPrice = offers.length > 0 ? Math.min(...offers.map(o => Number(o.price_uzs))) : 1000000;
        return res.status(200).json({
          id: Number(p.id),
          titleUz: p.title_uz,
          titleRu: p.title_ru || p.title_uz,
          titleEn: p.title_en || p.title_uz,
          brand: p.brand || 'General',
          storage: p.storage || '',
          ram: p.ram || '',
          color: p.color || '',
          descriptionUz: p.description_uz || '',
          imageUrl: p.image_url,
          lowestPriceUzs: lowestPrice,
          averagePriceUzs: lowestPrice,
          highestPriceUzs: lowestPrice,
          dealScore: 92,
          dealBadgeUz: '92/100 - YAXSHI NARX',
          category: { id: Number(p.category_id || 1), nameUz: p.cat_name_uz || 'Smartfonlar' },
          offers: offers.map(o => ({
            id: Number(o.id),
            store: { id: Number(o.store_id || 1), name: o.store_name || 'Uzum Market' },
            priceUzs: Number(o.price_uzs),
            isAvailable: o.is_available ?? true,
            offerUrl: o.offer_url || 'https://uzum.uz'
          }))
        });
      }

      let queryText = `
        SELECT p.*, c.name_uz as cat_name_uz, c.name_ru as cat_name_ru, c.name_en as cat_name_en
        FROM products p
        LEFT JOIN categories c ON p.category_id = c.id
        ORDER BY p.id DESC
      `;
      const products = await sql(queryText);

      const allOffers = await sql`
        SELECT po.*, s.name as store_name, s.logo_url as store_logo
        FROM product_offers po
        LEFT JOIN stores s ON po.store_id = s.id
      `;

      const formatted = products.map(p => {
        const prodOffers = allOffers.filter(o => String(o.product_id) === String(p.id));
        const lowestPrice = prodOffers.length > 0 ? Math.min(...prodOffers.map(o => Number(o.price_uzs))) : 1000000;
        return {
          id: Number(p.id),
          titleUz: p.title_uz,
          titleRu: p.title_ru || p.title_uz,
          titleEn: p.title_en || p.title_uz,
          brand: p.brand || 'General',
          storage: p.storage || '',
          ram: p.ram || '',
          color: p.color || '',
          descriptionUz: p.description_uz || '',
          imageUrl: p.image_url,
          lowestPriceUzs: lowestPrice,
          averagePriceUzs: lowestPrice,
          highestPriceUzs: lowestPrice,
          dealScore: 92,
          dealBadgeUz: '92/100 - YAXSHI NARX',
          category: { id: Number(p.category_id || 1), nameUz: p.cat_name_uz || 'Smartfonlar' },
          offers: prodOffers.map(o => ({
            id: Number(o.id),
            store: { id: Number(o.store_id || 1), name: o.store_name || 'Uzum Market' },
            priceUzs: Number(o.price_uzs),
            isAvailable: o.is_available ?? true,
            offerUrl: o.offer_url || 'https://uzum.uz'
          }))
        };
      });

      return res.status(200).json(formatted);
    }

    if (req.method === 'POST') {
      const b = req.body || {};
      const title = b.titleUz || b.title || 'Yangi Mahsulot';
      const brand = b.brand || 'General';
      const storage = b.storage || '';
      const ram = b.ram || '';
      const color = b.color || '';
      const description = b.descriptionUz || b.description || '';
      const imageUrl = b.imageUrl || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80';
      const catId = b.categoryId || (b.category ? b.category.id : 1);
      const price = Number(b.priceUzs) || 1000000;
      const storeName = b.storeName || 'Uzum Market';
      const storeOfferUrl = b.storeOfferUrl || 'https://uzum.uz';

      const insertRes = await sql`
        INSERT INTO products (title_uz, title_ru, title_en, brand, model, storage, ram, color, description_uz, description_ru, description_en, image_url, category_id, created_at)
        VALUES (${title}, ${title}, ${title}, ${brand}, ${title}, ${storage}, ${ram}, ${color}, ${description}, ${description}, ${description}, ${imageUrl}, ${catId}, NOW())
        RETURNING *
      `;
      const newProd = insertRes[0];

      // Insert store offer
      await sql`
        INSERT INTO product_offers (product_id, store_id, price_uzs, is_available, offer_url, updated_at)
        VALUES (${newProd.id}, 1, ${price}, true, ${storeOfferUrl}, NOW())
      `;

      return res.status(201).json({
        id: Number(newProd.id),
        titleUz: newProd.title_uz,
        brand: newProd.brand,
        storage: newProd.storage,
        ram: newProd.ram,
        color: newProd.color,
        descriptionUz: newProd.description_uz,
        imageUrl: newProd.image_url,
        lowestPriceUzs: price,
        averagePriceUzs: price,
        highestPriceUzs: price,
        dealScore: 92,
        dealBadgeUz: '92/100 - YAXSHI NARX',
        category: { id: Number(newProd.category_id || 1), nameUz: 'Smartfonlar' },
        offers: [{
          id: Date.now(),
          store: { id: 1, name: storeName },
          priceUzs: price,
          isAvailable: true,
          offerUrl: storeOfferUrl
        }]
      });
    }

    if (req.method === 'DELETE') {
      const { id } = req.query;
      if (id) {
        await sql`DELETE FROM product_offers WHERE product_id = ${id}`;
        await sql`DELETE FROM products WHERE id = ${id}`;
        return res.status(200).json({ status: 'DELETED', id });
      }
      return res.status(400).json({ error: 'Missing ID' });
    }

    res.status(405).json({ error: 'Method Not Allowed' });
  } catch (error) {
    console.error('API Error:', error);
    res.status(500).json({ error: error.message || 'Internal Server Error' });
  }
}
