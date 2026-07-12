// Server entry used exclusively by the build-time prerender step (see
// `vite-plugin-prerender-landing.js`). It renders ONLY the public marketing
// landing route to static HTML so crawlers receive the primary marketing
// content in the initial HTML response without executing client-side JS
// (Requirement 3.7). Authenticated application routes are never rendered here
// and are therefore excluded from prerendering (Requirement 3.8).
//
// This module is loaded through Vite's SSR pipeline (so JSX/ESM/`import.meta`
// all work) and executed in Node — there is no browser involved, so the
// prerender never downloads a headless Chromium.

import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider } from '../auth/AuthContext';
import { ThemeProvider } from '../theme/ThemeContext';
import LandingPage from '../pages/LandingPage';
import { LANDING_JSON_LD } from '../seo/landingSeo';

// The landing route lives at the bare apex path "/". We render it inside a
// MemoryRouter pinned to that path plus the AuthProvider the page depends on.
// `renderToStaticMarkup` does not run effects (so `applyLandingSeo` and the
// AuthProvider bootstrap fetch never fire here); the SEO tags that effect would
// add are injected by the plugin from the exported constants below instead.
export function renderLanding() {
  const bodyHtml = renderToStaticMarkup(
    <MemoryRouter initialEntries={['/']}>
      <AuthProvider>
        <ThemeProvider>
          <LandingPage />
        </ThemeProvider>
      </AuthProvider>
    </MemoryRouter>,
  );

  // JSON-LD structured data (Organization + SoftwareApplication) that the live
  // page injects via `applyLandingSeo`; emitted here so it is present in the
  // static HTML too.
  const jsonLd = `<script type="application/ld+json">${JSON.stringify(
    LANDING_JSON_LD,
  )}</script>`;

  return { bodyHtml, headTags: jsonLd };
}

export default renderLanding;
