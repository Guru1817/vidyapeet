/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
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
