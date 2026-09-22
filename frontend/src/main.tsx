import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from 'react-router-dom';
import { Toaster } from 'sonner';
import './styles.css';
import { AuthProvider } from './auth/AuthContext';
import { InstallAppFab } from './components/InstallAppFab';
import { registerPwa } from './pwa/registerPwa';
import { router } from './routes';

registerPwa();

/**
 * State ownership:
 * - TanStack Query: server/API state
 * - AuthContext: session identity (cookies hold tokens)
 * - Local state: ephemeral UI
 */
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
      refetchOnWindowFocus: false
    }
  }
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} future={{ v7_startTransition: true }} />
        <InstallAppFab />
        <Toaster richColors position="top-right" />
      </AuthProvider>
    </QueryClientProvider>
  </React.StrictMode>
);
