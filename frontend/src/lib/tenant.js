// Resolves the institute slug that identifies which branded portal we're on.
// Priority: subdomain (production) -> ?tenant= query (local dev) -> last used.
export function getTenantSlug() {
  const host = window.location.hostname;
  const labels = host.split('.');
  let sub = null;

  if (host.endsWith('.localhost') && labels.length >= 2) {
    // e.g. demo.localhost
    sub = labels[0];
  } else if (host !== 'localhost' && !/^\d+(\.\d+)*$/.test(host) && labels.length > 2) {
    // e.g. demo.vidyapeet.com (ignore apex + www)
    if (labels[0] !== 'www') {
      sub = labels[0];
    }
  }

  const params = new URLSearchParams(window.location.search);
  const fromQuery = params.get('tenant');
  const stored = localStorage.getItem('tenantSlug');

  const slug = sub || fromQuery || stored || null;
  if (slug) {
    localStorage.setItem('tenantSlug', slug);
  }
  return slug;
}

export function setTenantSlug(slug) {
  if (slug) {
    localStorage.setItem('tenantSlug', slug);
  }
}

export function clearTenantSlug() {
  localStorage.removeItem('tenantSlug');
}
