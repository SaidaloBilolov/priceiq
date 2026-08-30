declare global {
  interface Window {
    Telegram?: {
      WebApp: any;
    };
  }
}

export const getTelegramUser = () => {
  // 1. Try real Telegram WebApp object
  const webAppUser = window.Telegram?.WebApp?.initDataUnsafe?.user;
  if (webAppUser && webAppUser.id) {
    return {
      id: Number(webAppUser.id),
      first_name: webAppUser.first_name || '',
      username: webAppUser.username || '',
      language_code: webAppUser.language_code || 'uz'
    };
  }

  // 2. Check URL query parameters (useful when testing in browser e.g. ?tgId=12345&name=Ali)
  if (typeof window !== 'undefined') {
    const params = new URLSearchParams(window.location.search);
    const tgId = params.get('tgId') || params.get('id');
    if (tgId) {
      return {
        id: Number(tgId),
        first_name: params.get('name') || 'Telegram User',
        username: params.get('username') || 'user',
        language_code: params.get('lang') || 'uz'
      };
    }
  }

  // 3. Fallback for offline local dev testing
  return {
    id: 99887766,
    first_name: 'Foydalanuvchi',
    username: 'user',
    language_code: 'uz'
  };
};

export const initTelegramApp = () => {
  try {
    if (typeof window !== 'undefined' && window.Telegram?.WebApp) {
      window.Telegram.WebApp.ready();
      window.Telegram.WebApp.expand();
    }
  } catch (e) {
    console.warn('Telegram WebApp initialization error:', e);
  }
};
