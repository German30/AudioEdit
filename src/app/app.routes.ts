import { Routes } from '@angular/router';
import { adminGuard, authGuard, guestGuard } from './core/auth/auth.guards';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/marketing/landing.page').then((m) => m.LandingPage),
    title: 'Auralis — Edición de audio profesional',
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login.page').then((m) => m.LoginPage),
    title: 'Entrar · Auralis',
  },
  {
    path: 'oauth/callback',
    loadComponent: () => import('./features/auth/oauth-callback.page').then((m) => m.OauthCallbackPage),
    title: 'Conectando · Auralis',
  },
  {
    path: 'app',
    canActivate: [authGuard],
    loadComponent: () => import('./features/shell/app-shell.page').then((m) => m.AppShellPage),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.page').then((m) => m.DashboardPage),
        title: 'Proyectos · Auralis',
      },
      {
        path: 'assets',
        loadComponent: () => import('./features/assets/assets.page').then((m) => m.AssetsPage),
        title: 'Biblioteca · Auralis',
      },
      {
        path: 'subscription',
        loadComponent: () => import('./features/subscription/subscription.page').then((m) => m.SubscriptionPage),
        title: 'Plan y uso · Auralis',
      },
      {
        path: 'settings',
        loadComponent: () => import('./features/settings/settings.page').then((m) => m.SettingsPage),
        title: 'Ajustes · Auralis',
      },
      {
        path: 'admin',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/admin/admin.page').then((m) => m.AdminPage),
        title: 'Administración · Auralis',
      },
      {
        path: 'projects/:projectId',
        loadComponent: () => import('./features/editor/editor.page').then((m) => m.EditorPage),
        title: 'Editor · Auralis',
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
