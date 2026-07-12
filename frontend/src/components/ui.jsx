// Small, dependency-free UI primitives shared across pages.

export function Button({ variant = 'primary', className = '', type = 'button', ...props }) {
  const base =
    'inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition disabled:opacity-50 disabled:cursor-not-allowed';
  const variants = {
    // Brand-colored variants keep the institute's CSS-variable colors in both
    // appearances; only neutral variants get dark: overrides.
    primary: 'bg-brand text-white hover:bg-brand-dark',
    secondary:
      'bg-white text-slate-700 border border-slate-300 hover:bg-slate-50 dark:bg-slate-800 dark:text-slate-200 dark:border-slate-600 dark:hover:bg-slate-700',
    danger: 'bg-red-600 text-white hover:bg-red-700',
    ghost: 'text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-800',
  };
  return <button type={type} className={`${base} ${variants[variant]} ${className}`} {...props} />;
}

export function Card({ className = '', children }) {
  return (
    <div
      className={`rounded-xl border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-800 ${className}`}
    >
      {children}
    </div>
  );
}

export function CardBody({ className = '', children }) {
  return <div className={`p-5 ${className}`}>{children}</div>;
}

export function Field({ label, error, children }) {
  return (
    <label className="block">
      {label && (
        <span className="mb-1 block text-sm font-medium text-slate-700 dark:text-slate-200">{label}</span>
      )}
      {children}
      {error && <span className="mt-1 block text-xs text-red-600 dark:text-red-400">{error}</span>}
    </label>
  );
}

export function Input({ className = '', ...props }) {
  return (
    <input
      className={`w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-2 focus:ring-brand/30 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500 ${className}`}
      {...props}
    />
  );
}

export function Select({ className = '', children, ...props }) {
  return (
    <select
      className={`w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-2 focus:ring-brand/30 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100 ${className}`}
      {...props}
    >
      {children}
    </select>
  );
}

export function Textarea({ className = '', ...props }) {
  return (
    <textarea
      className={`w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-2 focus:ring-brand/30 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500 ${className}`}
      {...props}
    />
  );
}

export function Alert({ kind = 'error', children }) {
  if (!children) return null;
  const styles = {
    error: 'bg-red-50 text-red-700 border-red-200 dark:bg-red-950/40 dark:text-red-300 dark:border-red-900',
    success:
      'bg-green-50 text-green-700 border-green-200 dark:bg-green-950/40 dark:text-green-300 dark:border-green-900',
    info: 'bg-blue-50 text-blue-700 border-blue-200 dark:bg-blue-950/40 dark:text-blue-300 dark:border-blue-900',
  };
  return <div className={`rounded-lg border px-4 py-3 text-sm ${styles[kind]}`}>{children}</div>;
}

export function Badge({ children, kind = 'slate' }) {
  const styles = {
    slate: 'bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-200',
    green: 'bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-300',
    amber: 'bg-amber-100 text-amber-700 dark:bg-amber-900/50 dark:text-amber-300',
    blue: 'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300',
  };
  return (
    <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${styles[kind]}`}>
      {children}
    </span>
  );
}

export function EmptyState({ title, hint }) {
  return (
    <div className="rounded-xl border border-dashed border-slate-300 bg-white py-12 text-center dark:border-slate-600 dark:bg-slate-800">
      <p className="font-medium text-slate-600 dark:text-slate-300">{title}</p>
      {hint && <p className="mt-1 text-sm text-slate-400 dark:text-slate-500">{hint}</p>}
    </div>
  );
}
