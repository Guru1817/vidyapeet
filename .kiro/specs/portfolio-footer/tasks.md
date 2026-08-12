# Implementation Plan: Portfolio Footer

## Overview

Replace the existing inline footer in LandingPage with a standalone, reusable `PortfolioFooter` React component that features a permanently dark theme, copyright notice linking to the developer's portfolio (with referral param), and a gold-highlighted credit link. The component uses Tailwind utility classes for styling and responsive stacking behavior.

## Tasks

- [x] 1. Create PortfolioFooter component
  - [x] 1.1 Create `frontend/src/components/PortfolioFooter.jsx`
    - Implement a stateless React component that renders a semantic `<footer>` element
    - Use `bg-slate-900` background and `border-t border-slate-700` top border (always dark, independent of app theme)
    - Inside, render a flex container (`mx-auto flex max-w-6xl flex-col items-start justify-between gap-4 px-4 py-8 sm:flex-row sm:items-center`) for responsive two-column layout
    - Left section (Copyright_Section): render `© 2026 Gurupada Nayak. Made with ♥ in India` with "Gurupada Nayak" as an `<a>` linking to `https://gurupadanayak.in/?ref=vidyapeeth` with `target="_blank"`, `rel="noopener noreferrer"`, and an `aria-label`
    - Right section (Credit_Section): render the entire phrase "Crafted by GurupadaNayak →" as an `<a>` linking to `https://gurupadanayak.in` with `target="_blank"`, `rel="noopener noreferrer"`, and an `aria-label`; wrap "GurupadaNayak" in a `<span className="text-amber-400">`
    - Use `text-slate-300` for general text color
    - Export the component as default
    - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 5.1, 5.2, 5.3, 5.4, 6.3_

  - [x] 1.2 Replace existing footer in `frontend/src/pages/LandingPage.jsx`
    - Add `import PortfolioFooter from '../components/PortfolioFooter';` at the top
    - Remove the existing inline `<footer>...</footer>` block (the one with BrandLogo, copyright year, and contact email)
    - Render `<PortfolioFooter />` in its place
    - _Requirements: 6.1, 6.2_

- [x] 2. Checkpoint - Verify component renders correctly
  - Ensure the app builds without errors, ask the user if questions arise.

- [x] 3. Write unit tests for PortfolioFooter
  - [x] 3.1 Create `frontend/src/components/PortfolioFooter.test.jsx`
    - Test that the component renders a semantic `<footer>` element
    - Test that copyright text "© 2026 Gurupada Nayak. Made with ♥ in India" is present
    - Test that "Gurupada Nayak" link has href `https://gurupadanayak.in/?ref=vidyapeeth`
    - Test that the copyright link has `target="_blank"` and `rel="noopener noreferrer"`
    - Test that the copyright link has an accessible `aria-label`
    - Test that credit text "Crafted by GurupadaNayak →" is present
    - Test that credit link has href `https://gurupadanayak.in`
    - Test that the credit link has `target="_blank"` and `rel="noopener noreferrer"`
    - Test that the credit link has an accessible `aria-label`
    - Test that the developer name span has `text-amber-400` class
    - Test that the footer has `bg-slate-900` class for dark background
    - Test that the footer has `border-t` class for top border
    - Test that the container has `flex-col` and `sm:flex-row` classes for responsive stacking
    - _Requirements: 1.3, 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 5.1, 5.2, 5.3, 5.4_

  - [x] 3.2 Add integration test in `frontend/src/pages/LandingPage.test.jsx`
    - Verify that `<PortfolioFooter />` renders on the landing page
    - Verify the old footer content (Vidyapeeth copyright and contact email) is no longer present
    - _Requirements: 6.1, 6.2_

- [x] 4. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property-based tests are not applicable for this feature (static presentational component with no dynamic inputs)
- Unit tests use Vitest + React Testing Library (already configured in the project)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2"] },
    { "id": 2, "tasks": ["3.1", "3.2"] }
  ]
}
```
