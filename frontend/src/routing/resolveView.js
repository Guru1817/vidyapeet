// Pure request-view resolution for Track A (Vidyapeet V2).
//
// Decides whether a request should render the public marketing LANDING page
// or the institute PORTAL, based purely on (host, isAuthenticated). Keeping this
// a pure function makes the apex-vs-portal routing rule unit/property-testable.
//
// Rule (design "Request-view resolution"):
//   - If the host carries an institute subdomain slug -> PORTAL (regardless of auth).
//   - Otherwise (bare apex, ignoring `www`):
//       - unauthenticated -> LANDING
//       - authenticated   -> PORTAL

export const LANDING = 'LANDING';
export const PORTAL = 'PORTAL';

// Extracts the institute subdomain slug from a hostname, or null when the host
// is the bare apex (ignoring a leading `www`). Mirrors getTenantSlug()'s
// subdomain detection but as a pure function over the given host.
export function getSubdomainSlug(host) {
  if (!host) {
    return null;
  }

  // Strip a port if one is present (e.g. "demo.localhost:5173").
  const hostname = String(host).split(':')[0].toLowerCase();
  const labels = hostname.split('.');

  if (hostname.endsWith('.localhost') && labels.length >= 2) {
    // e.g. demo.localhost
    return labels[0];
  }

  // Real domains: e.g. demo.vidyapeeth.in (apex + TLD => length > 2).
  // Ignore bare "localhost", raw IP addresses, and the apex itself.
  if (hostname !== 'localhost' && !/^\d+(\.\d+)*$/.test(hostname) && labels.length > 2) {
    if (labels[0] !== 'www') {
      return labels[0];
    }
  }

  return null;
}

// Returns LANDING only for the bare apex (ignoring `www`) and an unauthenticated
// session; every institute-subdomain host, and every authenticated session,
// resolves to PORTAL.
export function resolveView(host, isAuthenticated) {
  const slug = getSubdomainSlug(host);
  if (slug) {
    return PORTAL;
  }
  return isAuthenticated ? PORTAL : LANDING;
}

export default resolveView;
