// Unit + snapshot tests for the code-based SVG BrandLogo component.
//
// Validates: Requirements 1.1, 1.2, 1.7, 1.8
//   1.1 - renders as scalable inline SVG markup
//   1.2 - composed of a distinct icon element and a "Vidyapeeth" wordmark that
//         can be rendered together or independently
//   1.7 - foreground uses currentColor so it adapts to light/dark appearance
//   1.8 - never overrides institute Brand_Theming variables (--brand/--brand-dark)
import { describe, it, expect, afterEach } from 'vitest';
import { render } from '@testing-library/react';
import BrandLogo from './BrandLogo.jsx';

// The <svg> is the container's first (and only) element child.
function getSvg(container) {
  return container.querySelector('svg');
}

describe('BrandLogo', () => {
  describe('variant rendering', () => {
    it("'full' renders both the icon and the wordmark", () => {
      const { container } = render(<BrandLogo variant="full" />);
      const svg = getSvg(container);

      // Scalable inline SVG with a viewBox that fits icon + wordmark (Req 1.1).
      expect(svg).toBeInTheDocument();
      expect(svg.tagName.toLowerCase()).toBe('svg');
      expect(svg).toHaveAttribute('viewBox', '0 0 200 40');

      // Icon sub-element present (the <g> stroke group with its paths) (Req 1.2).
      const iconGroup = svg.querySelector('g');
      expect(iconGroup).toBeInTheDocument();
      expect(iconGroup.querySelectorAll('path').length).toBeGreaterThan(0);

      // Wordmark sub-element present with the brand text (Req 1.2).
      const wordmark = svg.querySelector('text');
      expect(wordmark).toBeInTheDocument();
      expect(wordmark).toHaveTextContent('Vidyapeeth');
    });

    it("'icon' renders only the icon and no wordmark", () => {
      const { container } = render(<BrandLogo variant="icon" />);
      const svg = getSvg(container);

      expect(svg).toHaveAttribute('viewBox', '0 0 40 40');

      const iconGroup = svg.querySelector('g');
      expect(iconGroup).toBeInTheDocument();
      expect(iconGroup.querySelectorAll('path').length).toBeGreaterThan(0);

      // No wordmark text when rendered independently (Req 1.2).
      expect(svg.querySelector('text')).toBeNull();

      // Accessible title lives on the icon when it stands alone.
      expect(svg.querySelector('title')).toHaveTextContent('Vidyapeeth');
    });

    it("'wordmark' renders only the wordmark and no icon", () => {
      const { container } = render(<BrandLogo variant="wordmark" />);
      const svg = getSvg(container);

      expect(svg).toHaveAttribute('viewBox', '0 0 160 40');

      // No icon group when rendered independently (Req 1.2).
      expect(svg.querySelector('g')).toBeNull();

      const wordmark = svg.querySelector('text');
      expect(wordmark).toBeInTheDocument();
      expect(wordmark).toHaveTextContent('Vidyapeeth');
    });

    it('defaults to the full variant when no variant is provided', () => {
      const { container } = render(<BrandLogo />);
      const svg = getSvg(container);

      expect(svg).toHaveAttribute('viewBox', '0 0 200 40');
      expect(svg.querySelector('g')).toBeInTheDocument();
      expect(svg.querySelector('text')).toHaveTextContent('Vidyapeeth');
    });

    it('exposes an accessible image role and label', () => {
      const { container } = render(<BrandLogo variant="full" />);
      const svg = getSvg(container);
      expect(svg).toHaveAttribute('role', 'img');
      expect(svg).toHaveAttribute('aria-label', 'Vidyapeeth');
    });

    it('passes through className and extra props to the root svg', () => {
      const { container } = render(
        <BrandLogo variant="full" className="h-8 w-auto" data-testid="brand" />,
      );
      const svg = getSvg(container);
      expect(svg).toHaveClass('h-8', 'w-auto');
      expect(svg).toHaveAttribute('data-testid', 'brand');
    });
  });

  describe('color adaptation (Req 1.7)', () => {
    it('uses currentColor for foreground so it adapts to light/dark appearance', () => {
      const { container } = render(<BrandLogo variant="full" />);
      const svg = getSvg(container);

      // The stroke group and wordmark inherit color via currentColor rather
      // than a hard-coded fill, so the surrounding text color drives them.
      expect(svg.querySelector('g')).toHaveAttribute('stroke', 'currentColor');
      expect(svg.querySelector('text')).toHaveAttribute('fill', 'currentColor');

      // No hard-coded hex/named colors on the root svg (absent fill is fine,
      // it inherits currentColor).
      const rootFill = svg.getAttribute('fill') ?? '';
      expect(rootFill).not.toMatch(/#|rgb|black|white/i);
    });
  });

  describe('brand theming preservation (Req 1.8)', () => {
    afterEach(() => {
      document.documentElement.style.removeProperty('--brand');
      document.documentElement.style.removeProperty('--brand-dark');
    });

    it('renders without overriding active --brand / --brand-dark variables', () => {
      // Simulate an institute's configured brand theming.
      document.documentElement.style.setProperty('--brand', '#123456');
      document.documentElement.style.setProperty('--brand-dark', '#0a1a2a');

      const { container } = render(<BrandLogo variant="full" />);
      const svg = getSvg(container);

      // The institute's brand variables are untouched after rendering.
      expect(
        document.documentElement.style.getPropertyValue('--brand'),
      ).toBe('#123456');
      expect(
        document.documentElement.style.getPropertyValue('--brand-dark'),
      ).toBe('#0a1a2a');

      // The logo itself never sets the brand variables anywhere in its markup.
      const markup = svg.outerHTML;
      expect(markup).not.toContain('--brand');
    });
  });

  describe('snapshots', () => {
    it('matches the SVG output for the full variant', () => {
      const { container } = render(<BrandLogo variant="full" />);
      expect(getSvg(container)).toMatchSnapshot();
    });

    it('matches the SVG output for the icon variant', () => {
      const { container } = render(<BrandLogo variant="icon" />);
      expect(getSvg(container)).toMatchSnapshot();
    });

    it('matches the SVG output for the wordmark variant', () => {
      const { container } = render(<BrandLogo variant="wordmark" />);
      expect(getSvg(container)).toMatchSnapshot();
    });
  });
});
