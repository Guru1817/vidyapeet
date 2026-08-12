# Requirements Document

## Introduction

This feature replaces the existing landing page footer with a portfolio-style footer that credits and links to the developer's personal portfolio site (gurupadanayak.in). The footer uses a dark theme with a minimal, clean design. The left side displays a copyright notice with a heart symbol and a link to the portfolio (with a referral parameter), while the right side features the developer's name highlighted in gold/yellow as a link to the same portfolio.

## Glossary

- **Footer_Component**: The React component rendered at the bottom of public-facing pages that displays copyright information and portfolio links.
- **Portfolio_URL**: The developer's personal portfolio URL: `https://gurupadanayak.in`.
- **Referral_URL**: The Portfolio_URL appended with a query parameter indicating the referral source: `https://gurupadanayak.in/?ref=vidyapeeth`.
- **Landing_Page**: The public marketing page served at the apex domain (vidyapeeth.in) for unauthenticated visitors.
- **Copyright_Section**: The left-aligned portion of the Footer_Component containing the copyright text and referral link.
- **Credit_Section**: The right-aligned portion of the Footer_Component containing the developer name link.

## Requirements

### Requirement 1: Footer Layout Structure

**User Story:** As a visitor, I want to see a well-structured footer at the bottom of public pages, so that I can identify who built the platform.

#### Acceptance Criteria

1. THE Footer_Component SHALL render as a full-width section at the bottom of the Landing_Page.
2. THE Footer_Component SHALL use a two-column layout with the Copyright_Section on the left and the Credit_Section on the right.
3. WHEN the viewport width is smaller than the small breakpoint (640px), THE Footer_Component SHALL stack the Copyright_Section above the Credit_Section vertically.

### Requirement 2: Dark Theme Styling

**User Story:** As a visitor, I want the footer to have a dark, minimal design, so that it visually separates from the main content and looks professional.

#### Acceptance Criteria

1. THE Footer_Component SHALL render with a dark background color (slate-900 or equivalent dark tone) regardless of the application's current light or dark mode setting.
2. THE Footer_Component SHALL display text in a light color (white or light gray) to ensure readability against the dark background.
3. THE Footer_Component SHALL have a top border to visually separate it from the content above.

### Requirement 3: Copyright Section Content

**User Story:** As a visitor, I want to see the copyright notice with a personal touch, so that I know who owns the site and where to learn more.

#### Acceptance Criteria

1. THE Copyright_Section SHALL display the text "© 2026 Gurupada Nayak. Made with ♥ in India".
2. THE Copyright_Section SHALL render the name "Gurupada Nayak" as a clickable hyperlink.
3. WHEN a visitor clicks the "Gurupada Nayak" link in the Copyright_Section, THE Footer_Component SHALL navigate the visitor to the Referral_URL (https://gurupadanayak.in/?ref=vidyapeeth).
4. THE Copyright_Section link SHALL open in a new browser tab.
5. THE Copyright_Section link SHALL include `rel="noopener noreferrer"` for security.

### Requirement 4: Credit Section Content

**User Story:** As a visitor, I want to see who crafted the site with a clear call-to-action, so that I can visit the developer's portfolio.

#### Acceptance Criteria

1. THE Credit_Section SHALL display the text "Crafted by GurupadaNayak →".
2. THE Credit_Section SHALL render "GurupadaNayak" in a gold/yellow highlight color (amber-400 or equivalent) to visually distinguish the developer name from surrounding text.
3. THE Credit_Section SHALL render the entire phrase "Crafted by GurupadaNayak →" as a clickable hyperlink.
4. WHEN a visitor clicks the Credit_Section link, THE Footer_Component SHALL navigate the visitor to the Portfolio_URL (https://gurupadanayak.in).
5. THE Credit_Section link SHALL open in a new browser tab.
6. THE Credit_Section link SHALL include `rel="noopener noreferrer"` for security.

### Requirement 5: Accessibility

**User Story:** As a visitor using assistive technology, I want the footer links to be accessible, so that I can navigate and understand the footer content.

#### Acceptance Criteria

1. THE Footer_Component SHALL use a semantic `<footer>` HTML element.
2. THE Copyright_Section link SHALL have an accessible label that communicates the link destination.
3. THE Credit_Section link SHALL have an accessible label that communicates the link destination.
4. THE Footer_Component SHALL maintain a minimum contrast ratio of 4.5:1 between text and the dark background.

### Requirement 6: Footer Placement

**User Story:** As a developer, I want the portfolio footer to appear on public-facing pages, so that visitors see it without interfering with authenticated portal views.

#### Acceptance Criteria

1. THE Footer_Component SHALL appear on the Landing_Page.
2. THE Footer_Component SHALL replace the existing footer section currently rendered on the Landing_Page.
3. THE Footer_Component SHALL be implemented as a reusable React component so it can be included on additional public pages in the future.
