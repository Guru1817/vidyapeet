// Integration test for the build-time prerender output (Task 3.8).
//
// Requirements covered:
//   3.4 — a sitemap.xml listing only the public landing URL at the apex domain
//   3.5 — a robots.txt that allows the landing page and disallows authenticated routes
//   3.7 — the prerenderer emits crawlable static landing HTML whose primary
//         marketing content is present WITHOUT executing client-side JavaScript
//
// Approach: run the real production build (`npm run build`) once, then assert
// against the emitted `dist/` files by reading them straight off disk. Nothing
// in this test executes the client bundle or spins up a browser, so any
// marketing content we find in `dist/index.html` proves it was prerendered into
// the initial HTML response rather than produced by client-side JS.

import { execSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { beforeAll, describe, expect, it } from 'vitest';

const here = path.dirname(fileURLToPath(import.meta.url));
// src/prerender -> src -> frontend
const projectRoot = path.resolve(here, '..', '..');
const distDir = path.join(projectRoot, 'dist');

// Building includes a prerender pass that spins up an isolated Vite SSR server,
// so give it plenty of headroom on slow / cold CI machines.
const BUILD_TIMEOUT_MS = 240_000;

function read(file) {
  return readFileSync(path.join(distDir, file), 'utf8');
}

// Pull the inner HTML of the SPA mount node. If the prerender ran, this is the
// static landing markup; if it did not, it would be empty (`<div id="root">`).
function extractRootMarkup(html) {
  const match = html.match(/<div id="root">([\s\S]*?)<\/body>/);
  // Trim the trailing script tag(s) that follow the root div before </body>.
  const inner = match ? match[1] : '';
  return inner;
}

describe('prerender build output', () => {
  let indexHtml;
  let rootMarkup;

  beforeAll(() => {
    // Produce a fresh, deterministic build so the assertions never depend on a
    // stale dist/ left over from a previous run.
    execSync('npm run build', {
      cwd: projectRoot,
      stdio: 'inherit',
      timeout: BUILD_TIMEOUT_MS,
    });
    indexHtml = read('index.html');
    rootMarkup = extractRootMarkup(indexHtml);
  }, BUILD_TIMEOUT_MS + 30_000);

  it('emits the expected dist artifacts', () => {
    expect(existsSync(path.join(distDir, 'index.html'))).toBe(true);
    expect(existsSync(path.join(distDir, 'sitemap.xml'))).toBe(true);
    expect(existsSync(path.join(distDir, 'robots.txt'))).toBe(true);
  });

  describe('index.html (Requirement 3.7 — crawlable static landing HTML)', () => {
    it('prerenders landing markup into the SPA mount node (not an empty root)', () => {
      // The mount node must carry real markup — proof the content is in the
      // initial HTML rather than injected by client-side JS.
      expect(indexHtml).not.toMatch(/<div id="root">\s*<\/div>/);
      expect(rootMarkup.length).toBeGreaterThan(500);
    });

    it('contains the long-tail marketing phrase as static text', () => {
      // Present inside the prerendered body, not only in the <head> meta tags.
      expect(rootMarkup).toContain('mock test platform for coaching institutes');
    });

    it('contains core marketing content in the static body', () => {
      expect(rootMarkup).toContain('The mock test platform for coaching institutes');
      expect(rootMarkup).toContain('Everything you need to run online mock tests');
      expect(rootMarkup).toContain('Reusable question bank');
      expect(rootMarkup).toContain('Timed sections');
    });

    it('renders the footer contact email in the static body', () => {
      expect(rootMarkup).toContain('vidyapeeth.in@gmail.com');
    });

    it('embeds JSON-LD structured data for the organization and product', () => {
      expect(indexHtml).toContain('application/ld+json');
      expect(indexHtml).toContain('"@type":"Organization"');
      expect(indexHtml).toContain('"@type":"SoftwareApplication"');
    });

    it('includes the SEO title and meta description in the head', () => {
      expect(indexHtml).toContain(
        '<title>Vidyapeeth — Mock test platform for coaching institutes</title>',
      );
      expect(indexHtml).toMatch(/<meta\s+name="description"/);
    });
  });

  describe('sitemap.xml (Requirement 3.4 — apex landing URL only)', () => {
    let sitemap;
    beforeAll(() => {
      sitemap = read('sitemap.xml');
    });

    it('lists the apex landing URL', () => {
      expect(sitemap).toContain('<loc>https://vidyapeeth.in/</loc>');
    });

    it('lists exactly one URL', () => {
      const locs = sitemap.match(/<loc>/g) ?? [];
      expect(locs).toHaveLength(1);
    });

    it('does not list any authenticated application routes', () => {
      for (const route of ['/admin', '/student', '/superadmin', '/login']) {
        expect(sitemap).not.toContain(route);
      }
    });
  });

  describe('robots.txt (Requirement 3.5 — allow landing, disallow app routes)', () => {
    let robots;
    beforeAll(() => {
      robots = read('robots.txt');
    });

    it('allows crawling of the landing page', () => {
      expect(robots).toMatch(/Allow:\s*\/\$/);
    });

    it('disallows every authenticated application route', () => {
      expect(robots).toMatch(/Disallow:\s*\/admin/);
      expect(robots).toMatch(/Disallow:\s*\/student/);
      expect(robots).toMatch(/Disallow:\s*\/superadmin/);
      expect(robots).toMatch(/Disallow:\s*\/login/);
    });

    it('references the sitemap', () => {
      expect(robots).toContain('Sitemap: https://vidyapeeth.in/sitemap.xml');
    });
  });
});
