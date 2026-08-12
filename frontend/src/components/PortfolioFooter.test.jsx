import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import PortfolioFooter from './PortfolioFooter';

describe('PortfolioFooter', () => {
  it('renders a semantic <footer> element', () => {
    const { container } = render(<PortfolioFooter />);
    expect(container.querySelector('footer')).toBeInTheDocument();
  });

  it('displays copyright text "© 2026 Gurupada Nayak. Made with ♥ in India"', () => {
    render(<PortfolioFooter />);
    expect(screen.getByText(/© 2026/)).toBeInTheDocument();
    expect(screen.getByText(/Made with ♥ in India/)).toBeInTheDocument();
  });

  it('"Gurupada Nayak" link has href https://gurupadanayak.in/?ref=vidyapeeth', () => {
    render(<PortfolioFooter />);
    const link = screen.getByRole('link', { name: /Gurupada Nayak/i });
    expect(link).toHaveAttribute('href', 'https://gurupadanayak.in/?ref=vidyapeeth');
  });

  it('copyright link has target="_blank" and rel="noopener noreferrer"', () => {
    render(<PortfolioFooter />);
    const link = screen.getByRole('link', { name: /Gurupada Nayak/i });
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('copyright link has an accessible aria-label', () => {
    render(<PortfolioFooter />);
    const link = screen.getByRole('link', { name: /Gurupada Nayak/i });
    expect(link).toHaveAttribute('aria-label');
    expect(link.getAttribute('aria-label').length).toBeGreaterThan(0);
  });

  it('displays credit text "Crafted by GurupadaNayak →"', () => {
    render(<PortfolioFooter />);
    expect(screen.getByText(/Crafted by/)).toBeInTheDocument();
    expect(screen.getByText('GurupadaNayak')).toBeInTheDocument();
  });

  it('credit link has href https://gurupadanayak.in', () => {
    render(<PortfolioFooter />);
    const link = screen.getByRole('link', { name: /GurupadaNayak/i });
    expect(link).toHaveAttribute('href', 'https://gurupadanayak.in');
  });

  it('credit link has target="_blank" and rel="noopener noreferrer"', () => {
    render(<PortfolioFooter />);
    const link = screen.getByRole('link', { name: /GurupadaNayak/i });
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('credit link has an accessible aria-label', () => {
    render(<PortfolioFooter />);
    const link = screen.getByRole('link', { name: /GurupadaNayak/i });
    expect(link).toHaveAttribute('aria-label');
    expect(link.getAttribute('aria-label').length).toBeGreaterThan(0);
  });

  it('developer name span has text-amber-400 class', () => {
    render(<PortfolioFooter />);
    const span = screen.getByText('GurupadaNayak');
    expect(span).toHaveClass('text-amber-400');
  });

  it('footer has bg-slate-900 class for dark background', () => {
    const { container } = render(<PortfolioFooter />);
    const footer = container.querySelector('footer');
    expect(footer).toHaveClass('bg-slate-900');
  });

  it('footer has border-t class for top border', () => {
    const { container } = render(<PortfolioFooter />);
    const footer = container.querySelector('footer');
    expect(footer).toHaveClass('border-t');
  });

  it('container has flex-col and sm:flex-row classes for responsive stacking', () => {
    const { container } = render(<PortfolioFooter />);
    const div = container.querySelector('footer > div');
    expect(div).toHaveClass('flex-col');
    expect(div).toHaveClass('sm:flex-row');
  });
});
