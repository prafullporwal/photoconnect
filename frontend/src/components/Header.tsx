import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../lib/auth';

/**
 * Top navigation bar.
 *
 * <p>Sticky with a translucent "frosted glass" backdrop (Tailwind's
 * {@code backdrop-blur} stack). The page content scrolls underneath but the
 * nav remains glanceable — a small detail that signals "this app cares about
 * its surface."</p>
 *
 * <p>Role-aware: anonymous, customers, and photographers each see only the
 * links that make sense for them.</p>
 */
export function Header() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const navLinkClass = ({ isActive }: { isActive: boolean }) =>
    `relative px-3 py-2 text-sm font-medium transition ${
      isActive
        ? 'text-indigo-700'
        : 'text-slate-600 hover:text-slate-900'
    }`;

  const handleLogout = async () => {
    await logout();
    navigate('/', { replace: true });
  };

  return (
    <header className="sticky top-0 z-30 border-b border-slate-200/70 bg-white/75 backdrop-blur-lg supports-[backdrop-filter]:bg-white/60">
      <div className="mx-auto flex h-14 max-w-6xl items-center justify-between gap-6 px-4">
        {/* Logo — wordmark with a small gradient glyph */}
        <Link to="/" className="flex items-center gap-2">
          <span
            aria-hidden="true"
            className="grid h-8 w-8 place-items-center rounded-lg bg-gradient-to-br from-indigo-500 via-violet-500 to-fuchsia-500 text-white shadow-sm"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.2"
              strokeLinecap="round"
              strokeLinejoin="round"
              className="h-4 w-4"
            >
              {/* Simple camera glyph */}
              <path d="M14.5 4h-5L8 6H4a1 1 0 0 0-1 1v11a1 1 0 0 0 1 1h16a1 1 0 0 0 1-1V7a1 1 0 0 0-1-1h-4l-1.5-2Z" />
              <circle cx="12" cy="13" r="3.5" />
            </svg>
          </span>
          <span className="text-base font-bold tracking-tight text-slate-900">
            PhotoConnect
          </span>
        </Link>

        <nav className="flex flex-1 items-center gap-0.5">
          {/* Browse is hidden from logged-in photographers — they can't snoop on competitors */}
          {user?.role !== 'PHOTOGRAPHER' && (
            <NavLink to="/" end className={navLinkClass}>
              {({ isActive }) => (
                <>
                  Browse
                  {isActive && <ActiveUnderline />}
                </>
              )}
            </NavLink>
          )}

          {user?.role === 'PHOTOGRAPHER' && (
            <>
              <NavLink to="/me/profile" className={navLinkClass}>
                {({ isActive }) => (
                  <>
                    Profile
                    {isActive && <ActiveUnderline />}
                  </>
                )}
              </NavLink>
              <NavLink to="/me/portfolio" className={navLinkClass}>
                {({ isActive }) => (
                  <>
                    Portfolio
                    {isActive && <ActiveUnderline />}
                  </>
                )}
              </NavLink>
              <NavLink to="/me/availability" className={navLinkClass}>
                {({ isActive }) => (
                  <>
                    Calendar
                    {isActive && <ActiveUnderline />}
                  </>
                )}
              </NavLink>
              <NavLink to="/me/inbox" className={navLinkClass}>
                {({ isActive }) => (
                  <>
                    Inbox
                    {isActive && <ActiveUnderline />}
                  </>
                )}
              </NavLink>
            </>
          )}
          {user?.role === 'CUSTOMER' && (
            <>
              <NavLink to="/me/profile" className={navLinkClass}>
                {({ isActive }) => (
                  <>
                    Profile
                    {isActive && <ActiveUnderline />}
                  </>
                )}
              </NavLink>
              <NavLink to="/me/favorites" className={navLinkClass}>
                {({ isActive }) => (
                  <>
                    Saved
                    {isActive && <ActiveUnderline />}
                  </>
                )}
              </NavLink>
              <NavLink to="/me/inquiries" className={navLinkClass}>
                {({ isActive }) => (
                  <>
                    Inquiries
                    {isActive && <ActiveUnderline />}
                  </>
                )}
              </NavLink>
            </>
          )}
        </nav>

        <div className="flex items-center gap-2">
          {user ? (
            <>
              <span className="hidden items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600 sm:inline-flex">
                <span className="inline-block h-1.5 w-1.5 rounded-full bg-emerald-500" />
                {user.email}
              </span>
              <button
                onClick={handleLogout}
                className="rounded-md px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900"
              >
                Log out
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="rounded-md px-3 py-1.5 text-sm font-medium text-slate-600 hover:bg-slate-100 hover:text-slate-900"
              >
                Log in
              </Link>
              <Link
                to="/register"
                className="rounded-md bg-gradient-to-br from-indigo-600 to-violet-600 px-3.5 py-1.5 text-sm font-semibold text-white shadow-sm transition hover:shadow-md hover:brightness-110"
              >
                Sign up
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}

/**
 * Animated active-state underline. Sliding gradient bar beneath the active
 * nav item — beats the flat background-pill we had before.
 */
function ActiveUnderline() {
  return (
    <span
      aria-hidden="true"
      className="absolute inset-x-3 -bottom-px h-0.5 rounded-full bg-gradient-to-r from-indigo-500 to-violet-500"
    />
  );
}
