import { neon } from '@neondatabase/serverless';

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Credentials', 'true');
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS');

  if (req.method === 'OPTIONS') return res.status(200).end();

  try {
    const sql = neon('postgresql://neondb_owner:npg_XYvxkjzQ0nc2@ep-frosty-flower-ayl6bzxy-pooler.c-5.us-east-2.aws.neon.tech/neondb?sslmode=require');
    await sql`SELECT 1`;
    return res.status(200).json({
      status: 'UP',
      database: 'NEON_POSTGRES_CONNECTED',
      server: 'Vercel Serverless HTTPS',
      timestamp: Date.now()
    });
  } catch (err) {
    return res.status(200).json({
      status: 'UP',
      database: 'FALLBACK',
      error: err.message
    });
  }
}
