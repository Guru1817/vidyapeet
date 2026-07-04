import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import api from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { getTenantSlug } from '../lib/tenant';

const BrandingContext = createContext(null);

const DEFAULT_BRANDING = {
  name: 'Vidyapeet',
  slug: null,
  logoUrl: null,
  primaryColor: '#2563eb',
};

function darken(hex, amount = 0.18) {
  const m = /^#?([0-9a-f]{6})$/i.exec(hex || '');
  if (!m) return hex;
  const n = parseInt(m[1], 16);
  const r = Math.max(0, Math.round(((n >> 16) & 255) * (1 - amount)));
  const g = Math.max(0, Math.round(((n >> 8) & 255) * (1 - amount)));
  const b = Math.max(0, Math.round((n & 255) * (1 - amount)));
  return `#${((r << 16) | (g << 8) | b).toString(16).padStart(6, '0')}`;
}

function applyTheme(primaryColor) {
  const root = document.documentElement;
  root.style.setProperty('--brand', primaryColor);
  root.style.setProperty('--brand-dark', darken(primaryColor));
}

export function BrandingProvider({ children }) {
  const { user } = useAuth();
  const [branding, setBranding] = useState(DEFAULT_BRANDING);

  useEffect(() => {
    function useDefault() {
      setBranding(DEFAULT_BRANDING);
      applyTheme(DEFAULT_BRANDING.primaryColor);
      document.title = DEFAULT_BRANDING.name;
    }

    // Decide whose branding to show:
    //  - SUPER_ADMIN: the platform itself ("Vidyapeet"), never an institute.
    //  - INSTITUTE_ADMIN / STUDENT: their own institute.
    //  - Not logged in (login page): the portal slug from the URL.
    let slug = null;
    if (user) {
      if (user.role === 'SUPER_ADMIN') {
        useDefault();
        return;
      }
      slug = user.instituteSlug || null;
    } else {
      slug = getTenantSlug();
    }

    if (!slug) {
      useDefault();
      return;
    }

    api
      .get(`/api/branding/${slug}`)
      .then((res) => {
        const b = { ...DEFAULT_BRANDING, ...res.data };
        setBranding(b);
        applyTheme(b.primaryColor || DEFAULT_BRANDING.primaryColor);
        if (b.name) document.title = b.name;
      })
      .catch(() => useDefault());
  }, [user]);

  const value = useMemo(() => ({ branding }), [branding]);
  return <BrandingContext.Provider value={value}>{children}</BrandingContext.Provider>;
}

export function useBranding() {
  return useContext(BrandingContext)?.branding || DEFAULT_BRANDING;
}
