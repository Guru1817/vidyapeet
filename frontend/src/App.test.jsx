// Routing tests for App's apex-vs-portal wiring (Track A, task 3.4).
//
// Validates: Requirements 2.1, 2.2
//   2.1 - the apex domain without an authenticated session serves the LandingPage
//   2.2 - an institute subdomain serves the portal SPA, never the LandingPage;
//         and an authenticated apex session likewise renders the SPA
//
// App decides via resolveView(window.location.hostname, isAuthenticated):
//   LANDING  -> render LandingPage
//   PORTAL   -> render the existing SPA routes
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';

// Auth wiring: App only needs `user`; the landing/login pages also read the
// auth actions, so provide harmless stubs.
const authState = { user: null };
vi.mock('./auth/AuthContext', () => ({
  useAuth: () => ({ user: authState.user, login: vi.fn(), registerStudent: vi.fn() }),
}));

// Branding is consumed by LoginPage in the PORTAL branch.
vi.mock('./branding/BrandingContext', () => ({
  useBranding: () => ({ name: 'Vidyapeeth', logoUrl: null }),
}));

// Theme toggle is embedded in the login/landing headers; stub the theme hook so
// these routing tests don't need to mount the full ThemeProvider tree.
vi.mock('./theme/ThemeContext', () => ({
  useTheme: () => ({ theme: 'light', isDark: false, setTheme: vi.fn(), toggleTheme: vi.fn() }),
}));

import App from './App.jsx';

const LANDING_HEADING = /the mock test platform for coaching institutes/i;

let originalLocation;

function setHostname(hostname) {
  Object.defineProperty(window, 'location', {
    configurable: true,
    value: { hostname, search: '', pathname: '/', href: `http://${hostname}/` },
  });
}

beforeEach(() => {
  originalLocation = window.location;
  authState.user = null;
});

afterEach(() => {
  Object.defineProperty(window, 'location', { configurable: true, value: originalLocation });
});

describe('App apex-vs-portal routing', () => {
  it('serves the LandingPage at the bare apex for an unauthenticated visitor (Req 2.1)', () => {
    setHostname('vidyapeeth.in');
    authState.user = null;

    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { level: 1, name: LANDING_HEADING })).toBeInTheDocument();
  });

  it('serves the portal SPA (never the landing page) on an institute subdomain (Req 2.2)', () => {
    setHostname('demo.vidyapeeth.in');
    authState.user = null;

    render(
      <MemoryRouter initialEntries={['/login']}>
        <App />
      </MemoryRouter>,
    );

    // The landing hero must not appear on a subdomain...
    expect(screen.queryByRole('heading', { level: 1, name: LANDING_HEADING })).not.toBeInTheDocument();
    // ...instead the SPA's login route renders.
    expect(screen.getByText(/sign in to your account/i)).toBeInTheDocument();
  });

  it('serves the SPA for an authenticated session even at the apex (Req 2.2)', () => {
    setHostname('vidyapeeth.in');
    authState.user = { role: 'STUDENT' };

    render(
      <MemoryRouter initialEntries={['/login']}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.queryByRole('heading', { level: 1, name: LANDING_HEADING })).not.toBeInTheDocument();
    expect(screen.getByText(/sign in to your account/i)).toBeInTheDocument();
  });
});
