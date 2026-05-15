import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import './index.css';
import App from './App.tsx';
import { AuthProvider } from './lib/auth.tsx';
import { queryClient } from './lib/query.ts';

// Provider stack:
//   QueryClient → AuthProvider → BrowserRouter → App
//
// Order matters slightly: AuthProvider only needs React state, so it can sit
// inside QueryClient (auth doesn't use TanStack Query). The Router wraps the
// app so useNavigate/useLocation work everywhere.
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  </StrictMode>,
);
