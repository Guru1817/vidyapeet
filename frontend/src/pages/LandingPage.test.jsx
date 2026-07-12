// Unit + snapshot tests for the public marketing LandingPage.
//
// Validates: Requirements 2.3, 2.4, 2.5, 2.6, 2.7, 3.6, 3.9
//   2.3 - presents marketing content (who/what/features)
//   2.4 - student sign-up entry point submits to the student registration flow
//   2.5 - log-in entry point supports SUPER_ADMIN / INSTITUTE_ADMIN / STUDENT
//   2.6 - NO institute self-sign-up mechanism
//   2.7 - footer shows the contact email vidyapeeth.in@gmail.com
//   3.6 - semantic HTML landmark elements (header, nav, main, section, footer)
//   3.9 - long-tail phrase "mock test platform for coaching institutes"
//
// SEO meta tags / JSON-LD are covered by their own task; these tests focus on
// landing content, structure, and auth wiring.
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

// Auth wiring: capture the login / registerStudent calls the page makes.
const login = vi.fn();
const registerStudent = vi.fn();
vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ login, registerStudent }),
}));

// Navigation: LandingPage calls useNavigate() after a successful auth.
const navigate = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, useNavigate: () => navigate };
});

// The landing header embeds the Theme_Toggle; stub the theme hook so these
// content/wiring tests don't need to mount the full ThemeProvider tree.
vi.mock('../theme/ThemeContext', () => ({
  useTheme: () => ({ theme: 'light', isDark: false, setTheme: vi.fn(), toggleTheme: vi.fn() }),
}));

import LandingPage from './LandingPage.jsx';

beforeEach(() => {
  login.mockReset();
  registerStudent.mockReset();
  navigate.mockReset();
});

describe('LandingPage', () => {
  describe('marketing content (Req 2.3)', () => {
    it('describes who the platform is and what it offers', () => {
      render(<LandingPage />);

      // "Who" — a multi-tenant exam platform for coaching institutes.
      expect(
        screen.getByRole('heading', {
          level: 1,
          name: /the mock test platform for coaching institutes/i,
        }),
      ).toBeInTheDocument();
      expect(
        screen.getByText(/multi-tenant exam platform/i),
      ).toBeInTheDocument();

      // "What / features" — a features section listing capabilities.
      expect(
        screen.getByRole('heading', { name: /everything you need to run online mock tests/i }),
      ).toBeInTheDocument();
    });

    it('lists the core feature set', () => {
      render(<LandingPage />);
      const featureTitles = [
        /reusable question bank/i,
        /timed sections/i,
        /image-based questions/i,
        /instant results & leaderboards/i,
        /batches & students/i,
        /branded portal/i,
      ];
      for (const title of featureTitles) {
        expect(screen.getByRole('heading', { name: title })).toBeInTheDocument();
      }
    });
  });

  describe('long-tail SEO phrase (Req 3.9)', () => {
    it('includes the phrase "mock test platform for coaching institutes"', () => {
      render(<LandingPage />);
      const matches = screen.getAllByText(/mock test platform for coaching institutes/i);
      expect(matches.length).toBeGreaterThan(0);
    });
  });

  describe('semantic landmarks (Req 3.6)', () => {
    it('uses header, nav, main, section, and footer elements', () => {
      const { container } = render(<LandingPage />);

      expect(container.querySelector('header')).toBeInTheDocument();
      expect(container.querySelector('nav')).toBeInTheDocument();
      expect(container.querySelector('main')).toBeInTheDocument();
      expect(container.querySelector('footer')).toBeInTheDocument();
      expect(container.querySelectorAll('section').length).toBeGreaterThan(0);
    });

    it('exposes the corresponding ARIA landmark roles', () => {
      render(<LandingPage />);
      expect(screen.getByRole('banner')).toBeInTheDocument(); // <header>
      expect(screen.getByRole('navigation', { name: /primary/i })).toBeInTheDocument();
      expect(screen.getByRole('main')).toBeInTheDocument();
      expect(screen.getByRole('contentinfo')).toBeInTheDocument(); // <footer>
    });
  });

  describe('student sign-up wiring (Req 2.4, 2.8)', () => {
    it('defaults to the student sign-up form and submits to the register flow', async () => {
      const user = userEvent.setup();
      registerStudent.mockResolvedValue({ role: 'STUDENT' });
      const { container } = render(<LandingPage />);

      // Signup is the default mode: the name field only exists for signup.
      const nameField = screen.getByLabelText(/full name/i);
      expect(nameField).toBeInTheDocument();

      await user.type(screen.getByLabelText(/institute code/i), 'demo');
      await user.type(nameField, 'Asha Student');
      await user.type(screen.getByLabelText(/email/i), 'asha@example.com');
      await user.type(screen.getByLabelText(/password/i), 'secret123');
      // Submit the form via its submit control.
      await user.click(container.querySelector('button[type="submit"]'));

      expect(registerStudent).toHaveBeenCalledTimes(1);
      expect(registerStudent).toHaveBeenCalledWith({
        slug: 'demo',
        name: 'Asha Student',
        email: 'asha@example.com',
        password: 'secret123',
      });
      // login is the other flow and must not fire for a sign-up.
      expect(login).not.toHaveBeenCalled();
      // On success the student lands on the student home.
      expect(navigate).toHaveBeenCalledWith('/student/tests', { replace: true });
    });
  });

  describe('log-in wiring (Req 2.5)', () => {
    it('submits to the login flow and routes by role for all roles', async () => {
      const user = userEvent.setup();
      login.mockResolvedValue({ role: 'INSTITUTE_ADMIN' });
      const { container } = render(<LandingPage />);

      // Switch to login mode via a "Log in" control (several share the label).
      await user.click(screen.getAllByRole('button', { name: /^log in$/i })[0]);

      // Login mode drops the name field.
      expect(screen.queryByLabelText(/full name/i)).not.toBeInTheDocument();

      await user.type(screen.getByLabelText(/institute code/i), 'demo');
      await user.type(screen.getByLabelText(/email/i), 'admin@example.com');
      await user.type(screen.getByLabelText(/password/i), 'adminpass');
      await user.click(container.querySelector('button[type="submit"]'));

      expect(login).toHaveBeenCalledTimes(1);
      expect(login).toHaveBeenCalledWith({
        slug: 'demo',
        email: 'admin@example.com',
        password: 'adminpass',
      });
      expect(registerStudent).not.toHaveBeenCalled();
      expect(navigate).toHaveBeenCalledWith('/admin/dashboard', { replace: true });
    });

    it('routes a platform owner (SUPER_ADMIN) login to the superadmin home', async () => {
      const user = userEvent.setup();
      login.mockResolvedValue({ role: 'SUPER_ADMIN' });
      const { container } = render(<LandingPage />);

      await user.click(screen.getAllByRole('button', { name: /^log in$/i })[0]);
      // slug may be left blank for the platform owner.
      await user.type(screen.getByLabelText(/email/i), 'owner@example.com');
      await user.type(screen.getByLabelText(/password/i), 'ownerpass');
      await user.click(container.querySelector('button[type="submit"]'));

      expect(login).toHaveBeenCalledWith({
        slug: '',
        email: 'owner@example.com',
        password: 'ownerpass',
      });
      expect(navigate).toHaveBeenCalledWith('/superadmin/institutes', { replace: true });
    });
  });

  describe('no institute self-sign-up (Req 2.6)', () => {
    it('offers only a student account sign-up, not an institute one', () => {
      render(<LandingPage />);

      // The only "create account" controls are for a student.
      expect(
        screen.getAllByRole('button', { name: /create (your )?student account/i }).length,
      ).toBeGreaterThan(0);

      // No control lets a visitor register / create an institute themselves.
      expect(
        screen.queryByRole('button', {
          name: /(create|register|sign up).*(institute|coaching|organi[sz]ation)/i,
        }),
      ).not.toBeInTheDocument();
    });

    it('directs institutes to contact the platform owner instead of self-serving', () => {
      render(<LandingPage />);
      // Institutes are onboarded by getting in touch, not by self sign-up.
      expect(screen.getByRole('link', { name: /get in touch/i })).toHaveAttribute(
        'href',
        'mailto:vidyapeeth.in@gmail.com',
      );
    });
  });

  describe('footer contact email (Req 2.7)', () => {
    it('shows the institute contact email in the footer', () => {
      render(<LandingPage />);
      const footer = screen.getByRole('contentinfo');
      const emailLink = within(footer).getByRole('link', {
        name: /vidyapeeth\.in@gmail\.com/i,
      });
      expect(emailLink).toHaveAttribute('href', 'mailto:vidyapeeth.in@gmail.com');
    });
  });

  describe('snapshot', () => {
    it('matches the rendered landing markup', () => {
      const { container } = render(<LandingPage />);
      expect(container.firstChild).toMatchSnapshot();
    });
  });
});
