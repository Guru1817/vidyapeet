import { createServer } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';
import { readFile, writeFile } from 'node:fs/promises';

// Build-time prerender for the public landing route ONLY (Requirements 3.7, 3.8).
//
// After the normal client build finishes, this plugin renders `LandingPage` to
// static HTML and folds it into the generated `dist/index.html`, so the apex
// landing page ships crawlable marketing content in its initial HTML response
// without requiring client-side JavaScript.
//
// Design choice: instead of a headless-browser prerenderer (vite-plugin-prerender
// / puppeteer) — which would download Chromium and strain the free tier — this
// uses React's built-in server renderer driven through Vite's own SSR module
// loader. No extra runtime dependency and no browser download.
//
// Scope guarantees:
//   - It only ever renders the landing route ("/"), writing a single file
//     (`index.html`). The authenticated SPA routes (/admin, /student,
//     /superadmin, /login) are NOT enumerated or rendered, so they are excluded
//     from prerendering. They keep loading purely as the client-side SPA, and
//     `robots.txt` / `sitemap.xml` already keep them out of crawlers.

const ENTRY = '/src/prerender/entryLanding.jsx';

export default function prerenderLanding() {
  let outDir = 'dist';

  return {
    name: 'prerender-landing',
    // Only meaningful for the client build; skip SSR builds entirely.
    apply: 'build',

    configResolved(config) {
      // Don't run while building an SSR bundle.
      if (config.build?.ssr) {
        outDir = null;
        return;
      }
      outDir = path.resolve(config.root, config.build.outDir);
    },

    async closeBundle() {
      if (!outDir) {
        return;
      }

      const indexPath = path.join(outDir, 'index.html');
      let template;
      try {
        template = await readFile(indexPath, 'utf8');
      } catch {
        // No client index.html (e.g. SSR build) — nothing to prerender.
        return;
      }

      // Spin up an isolated Vite SSR server purely to load and render the
      // landing entry. `configFile: false` avoids re-entering this plugin.
      const ssrServer = await createServer({
        configFile: false,
        appType: 'custom',
        logLevel: 'warn',
        server: { middlewareMode: true },
        plugins: [react()],
      });

      try {
        const { renderLanding } = await ssrServer.ssrLoadModule(ENTRY);
        const { bodyHtml, headTags } = renderLanding();

        let html = template;

        // Inject the prerendered markup into the SPA mount node so the initial
        // HTML carries the landing content.
        html = html.replace(
          /<div id="root">\s*<\/div>/,
          `<div id="root">${bodyHtml}</div>`,
        );

        // Add JSON-LD structured data before </head> (only once).
        if (headTags && !html.includes('application/ld+json')) {
          html = html.replace('</head>', `    ${headTags}\n  </head>`);
        }

        await writeFile(indexPath, html, 'utf8');
        this.info?.('prerendered landing route -> index.html');
        // eslint-disable-next-line no-console
        console.log('\n[prerender-landing] Emitted static HTML for the landing route (index.html).');
      } finally {
        await ssrServer.close();
      }
    },
  };
}
