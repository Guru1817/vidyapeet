import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { homeFor } from '../auth/ProtectedRoute';
import { errorMessage } from '../api/client';
import BrandLogo from '../components/BrandLogo';
import ThemeToggle from '../theme/ThemeToggle';
import { applyLandingSeo } from '../seo/landingSeo';
import { Alert, Button, Card, CardBody, Field, Input } from '../components/ui';

// Public marketing landing page served at the apex domain (vidyapeeth.in) for
// unauthenticated visitors. It explains who the platform is, what it offers,
// and its features, and provides:
//   - a student sign-up control -> POST /api/auth/register (creates a STUDENT)
//   - a log-in control -> POST /api/auth/login (SUPER_ADMIN / INSTITUTE_ADMIN / STUDENT)
// There is intentionally NO institute self-sign-up; institutes are onboarded by
// contacting the platform owner at the footer email.
//
// SEO meta/JSON-LD and app routing are handled by their own tasks; this
// component focuses on semantic structure, marketing copy, and auth wiring.

const CONTACT_EMAIL = 'vidyapeeth.in@gmail.com';

const FEATURES = [
  {
    title: 'Reusable question bank',
    body: 'Build a per-institute bank of questions once and reuse them across many mock tests by reference — edit a question and every test that uses it stays in sync.',
  },
  {
    title: 'Timed sections',
    body: 'Organize each test into labeled sections while the whole attempt runs on a single overall timer, just like real competitive exams.',
  },
  {
    title: 'Image-based questions',
    body: 'Attach diagrams and figures to questions so you can set up physics, chemistry, and aptitude problems that need a visual.',
  },
  {
    title: 'Instant results & leaderboards',
    body: 'Auto-graded attempts give students immediate scores and ranks, while admins track batch and per-student performance.',
  },
  {
    title: 'Batches & students',
    body: 'Group students into batches, assign tests, and manage enrollments from one place.',
  },
  {
    title: 'Your own branded portal',
    body: 'Every institute gets a branded subdomain with its own logo and colors — a mock test platform that feels like yours.',
  },
];

export default function LandingPage() {
  const { login, registerStudent } = useAuth();
  const navigate = useNavigate();

  const [mode, setMode] = useState('signup'); // 'signup' | 'login'
  const [slug, setSlug] = useState('');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Inject on-page SEO (title, meta description, Open Graph, JSON-LD) while the
  // landing page is mounted; restore the prior head state on unmount.
  useEffect(() => applyLandingSeo(), []);

  function switchMode(next) {
    setMode(next);
    setError('');
  }

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
      setError(
        errorMessage(
          err,
          mode === 'login'
            ? 'Could not sign in. Please check your details.'
            : 'Could not create your account. Please check your details.',
        ),
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen flex-col bg-slate-50 text-slate-800 dark:bg-slate-900 dark:text-slate-200">
      <header className="border-b border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-800">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4">
          <a href="#top" className="flex items-center text-brand" aria-label="Vidyapeeth home">
            <BrandLogo variant="full" className="h-8 w-auto" />
          </a>
          <nav aria-label="Primary" className="flex items-center gap-3">
            <a
              href="#features"
              className="hidden text-sm font-medium text-slate-600 hover:text-brand dark:text-slate-300 sm:inline"
            >
              Features
            </a>
            <Button variant="ghost" onClick={() => switchMode('login')}>
              Log in
            </Button>
            <Button onClick={() => switchMode('signup')}>Sign up</Button>
            {/* Theme toggle available to logged-out visitors (Req 4.1). */}
            <ThemeToggle />
          </nav>
        </div>
      </header>

      <main id="top" className="flex-1">
        <section
          aria-labelledby="hero-heading"
          className="border-b border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-800"
        >
          <div className="mx-auto grid max-w-6xl items-center gap-10 px-4 py-14 md:grid-cols-2">
            <div>
              <p className="mb-3 text-sm font-semibold uppercase tracking-wide text-brand">
                Vidyapeeth
              </p>
              <h1
                id="hero-heading"
                className="text-3xl font-bold leading-tight text-slate-900 dark:text-white sm:text-4xl"
              >
                The mock test platform for coaching institutes
              </h1>
              <p className="mt-4 text-lg text-slate-600 dark:text-slate-300">
                Vidyapeeth is a multi-tenant exam platform that gives every coaching institute in India its
                own branded portal to create tests, manage batches of students, and deliver auto-graded mock
                exams with instant results.
              </p>
              <div className="mt-6 flex flex-wrap gap-3">
                <Button onClick={() => switchMode('signup')}>Create your student account</Button>
                <Button variant="secondary" onClick={() => switchMode('login')}>
                  Log in
                </Button>
              </div>
              <p className="mt-4 text-sm text-slate-500 dark:text-slate-400">
                Run a coaching institute?{' '}
                <a href={`mailto:${CONTACT_EMAIL}`} className="font-medium text-brand hover:underline">
                  Get in touch
                </a>{' '}
                to set up your institute portal.
              </p>
            </div>

            <div id="auth" className="w-full">
              <Card>
                <CardBody>
                  <div className="mb-4">
                    <h2 className="text-xl font-semibold text-slate-800 dark:text-slate-100">
                      {mode === 'login' ? 'Log in to your portal' : 'Create your student account'}
                    </h2>
                    <p className="text-sm text-slate-500 dark:text-slate-400">
                      {mode === 'login'
                        ? 'For students, institute admins, and the platform owner.'
                        : 'Join your institute using the code they gave you.'}
                    </p>
                  </div>

                  <form onSubmit={handleSubmit} className="space-y-4">
                    {error && <Alert>{error}</Alert>}

                    <Field label={mode === 'login' ? 'Institute code' : 'Institute code'}>
                      <Input
                        value={slug}
                        onChange={(e) => setSlug(e.target.value)}
                        placeholder={
                          mode === 'login'
                            ? 'e.g. demo (leave blank for platform owner)'
                            : 'e.g. demo (from your institute)'
                        }
                        required={mode === 'signup'}
                      />
                    </Field>

                    {mode === 'signup' && (
                      <Field label="Full name">
                        <Input value={name} onChange={(e) => setName(e.target.value)} required />
                      </Field>
                    )}

                    <Field label="Email">
                      <Input
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                      />
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
                      {loading
                        ? 'Please wait...'
                        : mode === 'login'
                          ? 'Log in'
                          : 'Create student account'}
                    </Button>
                  </form>

                  <div className="mt-4 text-center text-sm text-slate-500 dark:text-slate-400">
                    {mode === 'login' ? (
                      <button
                        type="button"
                        className="text-brand hover:underline"
                        onClick={() => switchMode('signup')}
                      >
                        New student? Create an account
                      </button>
                    ) : (
                      <button
                        type="button"
                        className="text-brand hover:underline"
                        onClick={() => switchMode('login')}
                      >
                        Already have an account? Log in
                      </button>
                    )}
                  </div>
                </CardBody>
              </Card>
            </div>
          </div>
        </section>

        <section aria-labelledby="about-heading" className="mx-auto max-w-6xl px-4 py-14">
          <h2 id="about-heading" className="text-2xl font-bold text-slate-900 dark:text-white">
            Built for coaching institutes and their students
          </h2>
          <p className="mt-3 max-w-3xl text-slate-600 dark:text-slate-300">
            Vidyapeeth is a "Shopify for coaching centers" — a software-as-a-service platform where each
            institute runs its own branded exam portal. Institute admins design mock tests and manage
            students; students practise, attempt timed tests, and see exactly where they stand.
          </p>
        </section>

        <section
          aria-labelledby="features-heading"
          id="features"
          className="border-t border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-800"
        >
          <div className="mx-auto max-w-6xl px-4 py-14">
            <h2 id="features-heading" className="text-2xl font-bold text-slate-900 dark:text-white">
              Everything you need to run online mock tests
            </h2>
            <p className="mt-3 max-w-3xl text-slate-600 dark:text-slate-300">
              A complete mock test platform for coaching institutes: from a reusable question bank to timed,
              sectioned exams and instant, auto-graded results.
            </p>
            <ul className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {FEATURES.map((f) => (
                <li key={f.title}>
                  <Card className="h-full">
                    <CardBody>
                      <h3 className="text-base font-semibold text-slate-800 dark:text-slate-100">{f.title}</h3>
                      <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">{f.body}</p>
                    </CardBody>
                  </Card>
                </li>
              ))}
            </ul>
          </div>
        </section>
      </main>

      <footer className="border-t border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-800">
        <div className="mx-auto flex max-w-6xl flex-col items-start justify-between gap-4 px-4 py-8 sm:flex-row sm:items-center">
          <div className="flex items-center gap-3 text-slate-500 dark:text-slate-400">
            <BrandLogo variant="icon" className="h-7 w-7 text-brand" />
            <span className="text-sm">© {new Date().getFullYear()} Vidyapeeth</span>
          </div>
          <p className="text-sm text-slate-600 dark:text-slate-300">
            Institute sign-up requests and queries:{' '}
            <a href={`mailto:${CONTACT_EMAIL}`} className="font-medium text-brand hover:underline">
              {CONTACT_EMAIL}
            </a>
          </p>
        </div>
      </footer>
    </div>
  );
}
