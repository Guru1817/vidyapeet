import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useBranding } from '../branding/BrandingContext';

const ROLE_LABEL = {
  SUPER_ADMIN: 'Platform Owner',
  INSTITUTE_ADMIN: 'Admin',
  STUDENT: 'Student',
};

export default function PortalLayout({ navItems = [] }) {
  const { user, logout } = useAuth();
  const branding = useBranding();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <div className="min-h-screen">
      <header className="bg-brand text-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
          <div className="flex items-center gap-3">
            {branding.logoUrl ? (
              <img src={branding.logoUrl} alt={branding.name} className="h-9 w-9 rounded bg-white object-contain p-0.5" />
            ) : (
              <div className="flex h-9 w-9 items-center justify-center rounded bg-white/20 font-bold">
                {branding.name?.charAt(0) || 'V'}
              </div>
            )}
            <div>
              <p className="text-sm font-semibold leading-tight">{branding.name}</p>
              <p className="text-xs text-white/70">{ROLE_LABEL[user?.role] || ''} portal</p>
            </div>
          </div>
          <div className="flex items-center gap-4">
            <span className="hidden text-sm text-white/90 sm:inline">{user?.name}</span>
            <button
              onClick={handleLogout}
              className="rounded-lg bg-white/15 px-3 py-1.5 text-sm font-medium hover:bg-white/25"
            >
              Log out
            </button>
          </div>
        </div>
        {navItems.length > 0 && (
          <nav className="border-t border-white/15">
            <div className="mx-auto flex max-w-6xl gap-1 px-2">
              {navItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `px-4 py-2.5 text-sm font-medium transition ${
                      isActive ? 'border-b-2 border-white text-white' : 'text-white/75 hover:text-white'
                    }`
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </div>
          </nav>
        )}
      </header>
      <main className="mx-auto max-w-6xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
