/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  // Class-based dark mode: ThemeContext toggles the `dark` class on
  // document.documentElement, so `dark:` variants activate app-wide.
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // Driven by the institute's primary color via a CSS variable so each
        // portal themes itself. Falls back to a sensible default.
        brand: {
          DEFAULT: 'var(--brand)',
          dark: 'var(--brand-dark)',
        },
      },
    },
  },
  plugins: [],
};
