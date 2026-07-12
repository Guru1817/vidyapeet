// Dark mode UI tests for ThemeContext + ThemeToggle (Track A, task 4.6).
//
// Validates: Requirements 4.1, 4.2, 4.3, 4.6, 4.7, 4.8
//   4.1 - a Theme_Toggle is available to both non-logged-in and logged-in users
//   4.2 - a guest's selection is stored client-side in localStorage
//   4.3 - with no stored preference, prefers-color-scheme drives the appearance
//   4.6 - a logged-in user's server themePreference is applied on load
//   4.7 - dark appearance is applied via the Tailwind `dark` class on <html>
//   4.8 - --brand/--brand-dark CSS variables are preserved while dark is active
//
// The persistence policy lives in ThemeContext; ThemeToggle is a thin control.
// We mock useAuth (to drive guest vs logged-in state and server preference) and
// the api client (to observe the server-side theme sync), plus window.matchMedia.
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

// Auth wiring: a mutable holder so each test can pick guest vs logged-in.
const authState = { user: null };
vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ user: authState.user }),
}));

// API client: observe the PUT /api/auth/me/theme sync for logged-in users.
// `vi.hoisted` keeps the spy available inside the hoisted vi.mock factory.
const { put } = vi.hoisted(() => ({ put: vi.fn(() => Promise.resolve({ data: {} })) }));
vi.mock('../api/client', () => ({
  default: { put },
}));

import { ThemeProvider, useTheme } from './ThemeContext.jsx';
import ThemeToggle from './ThemeToggle.jsx';

// A tiny probe component to read the resolved theme from context.
function ThemeProbe() {
  const { theme, isDark } = useTheme();
  return <div data-testid="theme" data-dark={String(isDark)}>{theme}</div>;
}

// Install a window.matchMedia stub that reports the given dark preference.
function mockMatchMedia(prefersDark) {
  window.matchMedia = vi.fn().mockImplementation((query) => ({
    matches: query.includes('dark') ? prefersDark : false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }));
}

beforeEach(() => {
  authState.user = null;
  put.mockClear();
  localStorage.clear();
  // Reset the <html> class and any brand variables between tests.
  document.documentElement.classList.remove('dark');
  document.documentElement.style.removeProperty('--brand');
  document.documentElement.style.removeProperty('--brand-dark');
  // Default: system prefers light unless a test overrides it.
  mockMatchMedia(false);
});

afterEach(() => {
  document.documentElement.classList.remove('dark');
});

describe('Theme_Toggle presence (Req 4.1)', () => {
  it('renders the toggle for a guest (no logged-in user)', () => {
    authState.user = null;
    render(
      <ThemeProvider>
        <ThemeToggle />
      </ThemeProvider>,
    );
    expect(screen.getByRole('button', { name: /switch to (dark|light) mode/i })).toBeInTheDocument();
  });

  it('renders the toggle for a logged-in user', () => {
    authState.user = { role: 'STUDENT', themePreference: 'LIGHT' };
    render(
      <ThemeProvider>
        <ThemeToggle />
      </ThemeProvider>,
    );
    expect(screen.getByRole('button', { name: /switch to (dark|light) mode/i })).toBeInTheDocument();
  });
});

describe('guest localStorage persistence (Req 4.2, 4.7)', () => {
  it('writes the selection to localStorage and toggles the dark class', async () => {
    const user = userEvent.setup();
    authState.user = null; // guest
    render(
      <ThemeProvider>
        <ThemeToggle />
        <ThemeProbe />
      </ThemeProvider>,
    );

    // Starts light (system prefers light, nothing stored).
    expect(document.documentElement.classList.contains('dark')).toBe(false);
    expect(localStorage.getItem('theme')).toBeNull();

    // Toggle to dark.
    await user.click(screen.getByRole('button', { name: /switch to dark mode/i }));

    expect(localStorage.getItem('theme')).toBe('dark');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(screen.getByTestId('theme')).toHaveAttribute('data-dark', 'true');

    // A guest never syncs to the server.
    expect(put).not.toHaveBeenCalled();

    // Toggle back to light.
    await user.click(screen.getByRole('button', { name: /switch to light mode/i }));
    expect(localStorage.getItem('theme')).toBe('light');
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });
});

describe('prefers-color-scheme fallback (Req 4.3)', () => {
  it('applies dark appearance when the system prefers dark and nothing is stored', () => {
    authState.user = null;
    localStorage.clear();
    mockMatchMedia(true); // system prefers dark

    render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>,
    );

    expect(screen.getByTestId('theme')).toHaveTextContent('dark');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });

  it('applies light appearance when the system prefers light and nothing is stored', () => {
    authState.user = null;
    localStorage.clear();
    mockMatchMedia(false);

    render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>,
    );

    expect(screen.getByTestId('theme')).toHaveTextContent('light');
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });
});

describe('server theme applied on load (Req 4.6)', () => {
  it("applies a logged-in user's DARK themePreference on load", () => {
    // Even though nothing is stored and the system prefers light, the server
    // preference wins.
    authState.user = { role: 'STUDENT', themePreference: 'DARK' };
    localStorage.clear();
    mockMatchMedia(false);

    render(
      <ThemeProvider>
        <ThemeProbe />
      </ThemeProvider>,
    );

    expect(screen.getByTestId('theme')).toHaveTextContent('dark');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });

  it('syncs the theme to the server (PUT /api/auth/me/theme) when a logged-in user toggles', async () => {
    const user = userEvent.setup();
    authState.user = { role: 'STUDENT', themePreference: 'LIGHT' };

    render(
      <ThemeProvider>
        <ThemeToggle />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('button', { name: /switch to dark mode/i }));

    expect(put).toHaveBeenCalledTimes(1);
    expect(put).toHaveBeenCalledWith('/api/auth/me/theme', { theme: 'DARK' });
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });
});

describe('brand variables preserved while dark is active (Req 4.8)', () => {
  it('leaves --brand/--brand-dark untouched when dark mode is toggled on', async () => {
    const user = userEvent.setup();
    authState.user = null;

    // Simulate an institute's brand theming being active.
    document.documentElement.style.setProperty('--brand', '#123456');
    document.documentElement.style.setProperty('--brand-dark', '#0a1a2a');

    render(
      <ThemeProvider>
        <ThemeToggle />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('button', { name: /switch to dark mode/i }));

    // Dark class is applied...
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    // ...but the brand variables are exactly as configured.
    expect(document.documentElement.style.getPropertyValue('--brand')).toBe('#123456');
    expect(document.documentElement.style.getPropertyValue('--brand-dark')).toBe('#0a1a2a');
  });
});
