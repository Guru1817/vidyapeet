import { describe, it, expect } from 'vitest';
import fc from 'fast-check';
import { resolveView, getSubdomainSlug, LANDING, PORTAL } from './resolveView.js';

// Generator for a DNS label (institute slug): starts with a letter, then
// letters/digits/hyphens, and is never the reserved "www".
const labelArb = fc
  .stringMatching(/^[a-z][a-z0-9-]{0,20}$/)
  .filter((s) => s.length > 0 && s !== 'www');

// An optional ":port" suffix so the helper's port-stripping is exercised.
const portArb = fc.option(
  fc.integer({ min: 1, max: 65535 }).map((p) => `:${p}`),
  { nil: '' }
);

// Bare-apex hosts (no institute subdomain slug). These include the production
// apex, its www alias, localhost, and raw IPs.
const apexHostArb = fc
  .tuple(
    fc.constantFrom(
      'vidyapeeth.in',
      'www.vidyapeeth.in',
      'localhost',
      '127.0.0.1',
      '10.0.0.1'
    ),
    portArb
  )
  .map(([host, port]) => `${host}${port}`);

// Institute-subdomain hosts: "<slug>.vidyapeeth.in" or "<slug>.localhost".
const subdomainHostArb = fc
  .tuple(labelArb, fc.constantFrom('.vidyapeeth.in', '.localhost'), portArb)
  .map(([slug, apex, port]) => `${slug}${apex}${port}`);

describe('resolveView - Property 1', () => {
  // Feature: vidyapeeth-v2-upgrades, Property 1: Apex-vs-portal view resolution
  // Validates: Requirements 2.1, 2.2
  it('returns LANDING iff bare apex + unauthenticated; subdomains always PORTAL', () => {
    fc.assert(
      fc.property(
        // Either an apex host or a subdomain host, paired with an auth flag.
        fc.oneof(apexHostArb, subdomainHostArb),
        fc.boolean(),
        (host, isAuthenticated) => {
          const view = resolveView(host, isAuthenticated);
          const hasSlug = getSubdomainSlug(host) !== null;

          if (hasSlug) {
            // Institute subdomains always resolve to PORTAL regardless of auth (Req 2.2).
            expect(view).toBe(PORTAL);
          } else {
            // Bare apex: LANDING iff unauthenticated, otherwise PORTAL (Req 2.1).
            expect(view).toBe(isAuthenticated ? PORTAL : LANDING);
          }

          // LANDING holds if and only if bare apex AND unauthenticated.
          expect(view === LANDING).toBe(!hasSlug && !isAuthenticated);
        }
      ),
      { numRuns: 200 }
    );
  });
});
