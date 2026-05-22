import { Route, Routes, Navigate } from 'react-router-dom';
import { Header } from './components/Header';
import { ProtectedRoute } from './components/ProtectedRoute';
import { BrowsePage } from './pages/BrowsePage';
import { PhotographerDetailPage } from './pages/PhotographerDetailPage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { MyProfilePage } from './pages/MyProfilePage';
import { NewInquiryPage } from './pages/NewInquiryPage';
import { InquiryListPage } from './pages/InquiryListPage';
import { InquiryDetailPage } from './pages/InquiryDetailPage';
import { PortfolioPage } from './pages/PortfolioPage';
import { FavoritesPage } from './pages/FavoritesPage';
import { MyAvailabilityPage } from './pages/MyAvailabilityPage';

/**
 * Route map for the PhotoConnect SPA.
 *
 *   Public:
 *     /                       Browse photographers
 *     /photographers/:id      Single photographer
 *     /login, /register       Auth
 *
 *   Authenticated:
 *     /me/profile             Own profile editor (role-aware inside the page)
 *     /me/inquiries           Customer outbox     (CUSTOMER only)
 *     /me/inbox               Photographer inbox  (PHOTOGRAPHER only)
 *     /inquiries/new          Create inquiry      (CUSTOMER only)
 *     /inquiries/:id          Detail + status PATCH (either participant)
 *
 *   Role enforcement is in two layers:
 *     - ProtectedRoute's allow=[…] redirects wrong-role users client-side.
 *     - The gateway/services enforce authoritatively via @PreAuthorize.
 */
export default function App() {
  return (
    <div className="flex min-h-full flex-col">
      <Header />
      <main className="flex-1">
        <Routes>
          {/* Public */}
          <Route path="/" element={<BrowsePage />} />
          <Route path="/photographers/:id" element={<PhotographerDetailPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Any authenticated user */}
          <Route
            path="/me/profile"
            element={
              <ProtectedRoute>
                <MyProfilePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/inquiries/:id"
            element={
              <ProtectedRoute>
                <InquiryDetailPage />
              </ProtectedRoute>
            }
          />

          {/* Customer-only */}
          <Route
            path="/me/inquiries"
            element={
              <ProtectedRoute allow={['CUSTOMER']}>
                <InquiryListPage mode="outbox" />
              </ProtectedRoute>
            }
          />
          <Route
            path="/me/favorites"
            element={
              <ProtectedRoute allow={['CUSTOMER']}>
                <FavoritesPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/inquiries/new"
            element={
              <ProtectedRoute allow={['CUSTOMER']}>
                <NewInquiryPage />
              </ProtectedRoute>
            }
          />

          {/* Photographer-only */}
          <Route
            path="/me/inbox"
            element={
              <ProtectedRoute allow={['PHOTOGRAPHER']}>
                <InquiryListPage mode="inbox" />
              </ProtectedRoute>
            }
          />
          <Route
            path="/me/portfolio"
            element={
              <ProtectedRoute allow={['PHOTOGRAPHER']}>
                <PortfolioPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/me/availability"
            element={
              <ProtectedRoute allow={['PHOTOGRAPHER']}>
                <MyAvailabilityPage />
              </ProtectedRoute>
            }
          />

          {/* Catch-all → home */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
