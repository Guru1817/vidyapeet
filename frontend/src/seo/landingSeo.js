// On-page SEO for the public landing page (served at the apex domain).
//
// The project does not use react-helmet or any head-management library; the
// established convention (see BrandingContext) is to mutate `document` directly
// from an effect. This module centralises the landing page's <title>, meta
// description, Open Graph tags, and JSON-LD structured data so they can be
// applied on mount and cleaned up on unmount, and so the values can be asserted
// in tests. The build-time prerender step (separate task) serialises the DOM
// after these effects run, so the tags end up in the crawlable static HTML.

// The public landing page lives at the bare apex domain.
export const LANDING_URL = 'https://vidyapeeth.in/';
export const LANDING_IMAGE = 'https://vidyapeeth.in/favicon.svg';

export const LANDING_TITLE =
  'Vidyapeeth — Mock test platform for coaching institutes';

export const LANDING_DESCRIPTION =
  'Vidyapeeth is a mock test platform for coaching institutes in India. Every ' +
  'institute gets a branded portal to build a reusable question bank, run timed ' +
  'sectioned exams, and give students instant auto-graded results and leaderboards.';

// Open Graph tags: title, description, type, url, image (Requirement 3.2).
const OG_TAGS = [
  { property: 'og:title', content: LANDING_TITLE },
  { property: 'og:description', content: LANDING_DESCRIPTION },
  { property: 'og:type', content: 'website' },
  { property: 'og:url', content: LANDING_URL },
  { property: 'og:image', content: LANDING_IMAGE },
  { property: 'og:site_name', content: 'Vidyapeeth' },
];

const NAME_META = [
  { name: 'description', content: LANDING_DESCRIPTION },
];

// JSON-LD structured data describing the organization and the software product
// (Requirement 3.3).
export const LANDING_JSON_LD = [
  {
    '@context': 'https://schema.org',
    '@type': 'Organization',
    name: 'Vidyapeeth',
    url: LANDING_URL,
    logo: LANDING_IMAGE,
    description:
      'Multi-tenant mock test platform that gives coaching institutes in India ' +
      'their own branded exam portal.',
    email: 'vidyapeeth.in@gmail.com',
  },
  {
    '@context': 'https://schema.org',
    '@type': 'SoftwareApplication',
    name: 'Vidyapeeth',
    applicationCategory: 'EducationApplication',
    operatingSystem: 'Web',
    url: LANDING_URL,
    description: LANDING_DESCRIPTION,
    offers: {
      '@type': 'Offer',
      price: '0',
      priceCurrency: 'INR',
    },
  },
];

// Marker so we only ever touch tags this module created, and can remove them
// cleanly on unmount without disturbing tags owned by other code.
const MARKER = 'data-landing-seo';

function upsertMeta(selector, attrs) {
  let el = document.head.querySelector(selector);
  if (!el) {
    el = document.createElement('meta');
    el.setAttribute(MARKER, 'true');
    document.head.appendChild(el);
  }
  Object.entries(attrs).forEach(([k, v]) => el.setAttribute(k, v));
  return el;
}

function upsertCanonical(url) {
  let el = document.head.querySelector('link[rel="canonical"]');
  if (!el) {
    el = document.createElement('link');
    el.setAttribute('rel', 'canonical');
    el.setAttribute(MARKER, 'true');
    document.head.appendChild(el);
  }
  el.setAttribute('href', url);
  return el;
}

/**
 * Apply the landing page SEO tags to the document head.
 * Returns a cleanup function that restores the previous title and removes any
 * tags this call created.
 */
export function applyLandingSeo() {
  const previousTitle = document.title;
  document.title = LANDING_TITLE;

  NAME_META.forEach((m) => upsertMeta(`meta[name="${m.name}"]`, { name: m.name, content: m.content }));
  OG_TAGS.forEach((m) =>
    upsertMeta(`meta[property="${m.property}"]`, { property: m.property, content: m.content }),
  );
  upsertCanonical(LANDING_URL);

  const script = document.createElement('script');
  script.type = 'application/ld+json';
  script.setAttribute(MARKER, 'true');
  script.textContent = JSON.stringify(LANDING_JSON_LD);
  document.head.appendChild(script);

  return function cleanup() {
    document.title = previousTitle;
    document.head.querySelectorAll(`[${MARKER}]`).forEach((el) => el.remove());
  };
}
