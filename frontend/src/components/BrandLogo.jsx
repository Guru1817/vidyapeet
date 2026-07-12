// Code-based SVG brand logo for Vidyapeeth (icon + wordmark).
//
// The foreground uses `currentColor`, so the logo inherits the surrounding
// Tailwind text color and adapts to light/dark appearance (via `dark:` text
// variants) while remaining legible against either background. It never sets
// the `--brand` / `--brand-dark` CSS variables, so an institute's configured
// brand theming is preserved wherever the logo is rendered.
//
// Props:
//   variant  'full' (icon + wordmark, default) | 'icon' | 'wordmark'
//   className passthrough classes applied to the root <svg>

// The icon: an open-book "mortarboard" mark drawn with currentColor strokes.
function Icon({ title }) {
  return (
    <g fill="none" stroke="currentColor" strokeWidth="3" strokeLinejoin="round" strokeLinecap="round">
      {title && <title>{title}</title>}
      {/* graduation cap */}
      <path d="M20 15 L4 22 L20 29 L36 22 Z" />
      {/* cap tassel / stem */}
      <path d="M36 22 L36 30" />
      <circle cx="36" cy="32" r="1.6" fill="currentColor" stroke="none" />
      {/* open book base */}
      <path d="M8 26 L8 33 C12 30 16 30 20 32 C24 30 28 30 32 33 L32 26" />
    </g>
  );
}

export default function BrandLogo({ variant = 'full', className = '', ...props }) {
  const showIcon = variant === 'full' || variant === 'icon';
  const showWordmark = variant === 'full' || variant === 'wordmark';

  // Compose a viewBox that fits only the pieces being rendered.
  let viewBox = '0 0 200 40';
  if (variant === 'icon') viewBox = '0 0 40 40';
  if (variant === 'wordmark') viewBox = '0 0 160 40';

  return (
    <svg
      viewBox={viewBox}
      role="img"
      aria-label="Vidyapeeth"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      {...props}
    >
      {showIcon && <Icon title={variant === 'icon' ? 'Vidyapeeth' : undefined} />}
      {showWordmark && (
        <text
          x={variant === 'wordmark' ? 0 : 48}
          y="27"
          fill="currentColor"
          fontFamily="ui-sans-serif, system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif"
          fontSize="24"
          fontWeight="700"
          letterSpacing="-0.5"
        >
          Vidyapeeth
        </text>
      )}
    </svg>
  );
}
