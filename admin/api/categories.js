import { neon } from '@neondatabase/serverless';

const sql = neon('postgresql://neondb_owner:npg_XYvxkjzQ0nc2@ep-frosty-flower-ayl6bzxy-pooler.c-5.us-east-2.aws.neon.tech/neondb?sslmode=require');

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Credentials', 'true');
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', '*');

  if (req.method === 'OPTIONS') return res.status(200).end();

  try {
    const categories = await sql`SELECT * FROM categories ORDER BY id ASC`;
    if (categories.length === 0) {
      return res.status(200).json([
        { id: 1, nameUz: 'Smartfonlar', nameRu: 'Смартфоны', nameEn: 'Smartphones' },
        { id: 2, nameUz: 'Noutbuklar', nameRu: 'Ноутбуки', nameEn: 'Laptops' },
        { id: 3, nameUz: 'Maishiy Texnika', nameRu: 'Бытовая техника', nameEn: 'Appliances' },
        { id: 4, nameUz: 'Kiyim-kechak', nameRu: 'Одежда', nameEn: 'Clothing' },
        { id: 5, nameUz: 'Aksessuarlar', nameRu: 'Аксессуары', nameEn: 'Accessories' },
        { id: 6, nameUz: 'Boshqa', nameRu: 'Другое', nameEn: 'Other' }
      ]);
    }
    return res.status(200).json(categories.map(c => ({
      id: Number(c.id),
      nameUz: c.name_uz,
      nameRu: c.name_ru || c.name_uz,
      nameEn: c.name_en || c.name_uz
    })));
  } catch (err) {
    return res.status(200).json([
      { id: 1, nameUz: 'Smartfonlar', nameRu: 'Смартфоны', nameEn: 'Smartphones' },
      { id: 2, nameUz: 'Noutbuklar', nameRu: 'Ноутбуки', nameEn: 'Laptops' },
      { id: 3, nameUz: 'Maishiy Texnika', nameRu: 'Бытовая техника', nameEn: 'Appliances' },
      { id: 4, nameUz: 'Kiyim-kechak', nameRu: 'Одежда', nameEn: 'Clothing' },
      { id: 5, nameUz: 'Aksessuarlar', nameRu: 'Аксессуары', nameEn: 'Accessories' },
      { id: 6, nameUz: 'Boshqa', nameRu: 'Другое', nameEn: 'Other' }
    ]);
  }
}
