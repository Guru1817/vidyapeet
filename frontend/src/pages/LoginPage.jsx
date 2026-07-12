import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { homeFor } from '../auth/ProtectedRoute';
import { useBranding } from '../branding/BrandingContext';
import { getTenantSlug } from '../lib/tenant';
import { errorMessage } from '../api/client';
import { Alert, Button, Card, CardBody, Field, Input } from '../components/ui';
import BrandLogo from '../components/BrandLogo';
import ThemeToggle from '../theme/ThemeToggle';

export default function LoginPage() {
  const { login, registerStudent } = useAuth();
  const branding = useBranding();
  const navigate = useNavigate();

  const [mode, setMode] = useState('login'); // 'login' | 'register'
  const [slug, setSlug] = useState(getTenantSlug() || '');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const user =
        mode === 'login'
          ? await login({ slug: slug.trim(), email, password })
          : await registerStudent({ slug: slug.trim(), name, email, password });
      navigate(homeFor(user.role), { replace: true });
    } catch (err) {
      setError(errorMessage(err, 'Could not sign in. Please check your details.'));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center px-4">
      {/* Theme toggle available to logged-out users (Req 4.1). */}
      <div className="absolute right-4 top-4">
        <ThemeToggle />
      </div>
      <div className="w-full max-w-md">
        <div className="mb-6 text-center">
          {branding.logoUrl ? (
            <img src={branding.logoUrl} alt={branding.name} className="mx-auto mb-3 h-14 object-contain" />
          ) : (
            <BrandLogo variant="icon" className="mx-auto mb-3 h-14 w-14 text-brand" />
          )}
          <h1 className="text-2xl font-semibold text-slate-800 dark:text-slate-100">{branding.name}</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            {mode === 'login' ? 'Sign in to your account' : 'Create your student account'}
          </p>
        </div>

        <Card>
          <CardBody>
            <form onSubmit={handleSubmit} className="space-y-4">
              {error && <Alert>{error}</Alert>}

              <Field label="Institute code">
                <Input
                  value={slug}
                  onChange={(e) => setSlug(e.target.value)}
                  placeholder="e.g. demo (leave blank for platform owner)"
                />
              </Field>

              {mode === 'register' && (
                <Field label="Full name">
                  <Input value={name} onChange={(e) => setName(e.target.value)} required />
                </Field>
              )}

              <Field label="Email">
                <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
              </Field>

              <Field label="Password">
                <Input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </Field>

              <Button type="submit" className="w-full" disabled={loading}>
                {loading ? 'Please wait...' : mode === 'login' ? 'Sign in' : 'Create account'}
              </Button>
            </form>

            <div className="mt-4 text-center text-sm text-slate-500 dark:text-slate-400">
              {mode === 'login' ? (
                <button className="text-brand hover:underline" onClick={() => setMode('register')}>
                  New student? Create an account
                </button>
              ) : (
                <button className="text-brand hover:underline" onClick={() => setMode('login')}>
                  Already have an account? Sign in
                </button>
              )}
            </div>
          </CardBody>
        </Card>
      </div>
    </div>
  );
}
