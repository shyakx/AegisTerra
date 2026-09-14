import { createBrowserRouter, Navigate } from 'react-router-dom';
import { ProtectedRoute } from '../auth/ProtectedRoute';
import AppLayout from '../layouts/AppLayout';
import SiteLayout from '../layouts/SiteLayout';
import CropHistoryPage from '../pages/CropHistoryPage';
import CropsPage from '../pages/CropsPage';
import DashboardPage from '../pages/DashboardPage';
import FarmDetailsPage from '../pages/FarmDetailsPage';
import FarmPlotsPage from '../pages/FarmPlotsPage';
import FarmsPage from '../pages/FarmsPage';
import FarmerProfilePage from '../pages/FarmerProfilePage';
import FarmerRegistrationPage from '../pages/FarmerRegistrationPage';
import FarmersPage from '../pages/FarmersPage';
import ForgotPasswordPage from '../pages/ForgotPasswordPage';
import HouseholdsPage from '../pages/HouseholdsPage';
import LoginPage from '../pages/LoginPage';
import HomePage from '../pages/public/HomePage';
import AboutPage from '../pages/public/AboutPage';
import PlatformPage from '../pages/public/PlatformPage';
import PartnersPage from '../pages/public/PartnersPage';
import JoinPage from '../pages/public/JoinPage';
import ResetPasswordPage from '../pages/ResetPasswordPage';
import SeasonsPage from '../pages/SeasonsPage';
import SettingsPage from '../pages/SettingsPage';
import UsersPage from '../pages/UsersPage';
import ClimateDashboardPage from '../pages/ClimateDashboardPage';
import ClimateStationsPage from '../pages/ClimateStationsPage';
import ClimateStationDetailPage from '../pages/ClimateStationDetailPage';
import ClimateObservationsPage from '../pages/ClimateObservationsPage';
import ClimateImportJobsPage from '../pages/ClimateImportJobsPage';
import ClimateDatasetsPage from '../pages/ClimateDatasetsPage';
import NationalRiskDashboardPage from '../pages/NationalRiskDashboardPage';
import ClimateAlertsPage from '../pages/ClimateAlertsPage';
import FarmClimateIntelPage from '../pages/FarmClimateIntelPage';
import DistrictRiskPage from '../pages/DistrictRiskPage';
import ClimateIntelJobsPage from '../pages/ClimateIntelJobsPage';
import PlanningOutlookPage from '../pages/PlanningOutlookPage';

export const router = createBrowserRouter(
  [
    {
      element: <SiteLayout />,
      children: [
        { path: '/', element: <HomePage /> },
        { path: '/about', element: <AboutPage /> },
        { path: '/platform', element: <PlatformPage /> },
        { path: '/partners', element: <PartnersPage /> },
        { path: '/join', element: <JoinPage /> }
      ]
    },
    { path: '/login', element: <LoginPage /> },
    { path: '/forgot-password', element: <ForgotPasswordPage /> },
    { path: '/reset-password', element: <ResetPasswordPage /> },
    {
      element: <ProtectedRoute />,
      children: [
        {
          element: <AppLayout />,
          children: [
            { path: 'app', element: <DashboardPage /> },
            {
              element: <ProtectedRoute permission="users:read" />,
              children: [{ path: 'users', element: <UsersPage /> }]
            },
            {
              element: <ProtectedRoute permission="farmers:read" />,
              children: [
                {
                  element: <ProtectedRoute denyRoles={['FARMER']} />,
                  children: [{ path: 'households', element: <HouseholdsPage /> }]
                },
                { path: 'farmers', element: <FarmersPage /> },
                {
                  element: <ProtectedRoute permission="farmers:write" />,
                  children: [{ path: 'farmers/register', element: <FarmerRegistrationPage /> }]
                },
                { path: 'farmers/:id', element: <FarmerProfilePage /> }
              ]
            },
            {
              element: <ProtectedRoute permission="farms:read" />,
              children: [
                { path: 'farms', element: <FarmsPage /> },
                { path: 'farms/:id', element: <FarmDetailsPage /> },
                { path: 'farms/:id/boundary', element: <Navigate to=".." relative="path" replace /> },
                { path: 'farms/:id/plots', element: <FarmPlotsPage /> },
                { path: 'farms/:id/crop-history', element: <CropHistoryPage /> },
                {
                  element: <ProtectedRoute denyRoles={['FARMER']} />,
                  children: [
                    { path: 'crops', element: <CropsPage /> },
                    { path: 'seasons', element: <SeasonsPage /> }
                  ]
                }
              ]
            },
            {
              path: 'notifications',
              element: <Navigate to="/app" replace />
            },
            {
              path: 'notifications/:id',
              element: <Navigate to="/app" replace />
            },
            {
              path: 'notification-preferences',
              element: <Navigate to="/settings" replace />
            },
            {
              element: <ProtectedRoute permission="climate:read" />,
              children: [
                { path: 'climate', element: <ClimateDashboardPage /> },
                { path: 'climate/stations', element: <ClimateStationsPage /> },
                { path: 'climate/stations/:id', element: <ClimateStationDetailPage /> },
                { path: 'climate/observations', element: <ClimateObservationsPage /> },
                { path: 'climate/import-jobs', element: <ClimateImportJobsPage /> },
                { path: 'climate/datasets', element: <ClimateDatasetsPage /> },
                { path: 'climate/map', element: <Navigate to="/climate" replace /> }
              ]
            },
            { path: 'planning', element: <PlanningOutlookPage /> },
            {
              element: <ProtectedRoute permission="climate-intel:read" />,
              children: [
                { path: 'climate-intel', element: <NationalRiskDashboardPage /> },
                { path: 'climate-intel/alerts', element: <ClimateAlertsPage /> },
                { path: 'climate-intel/jobs', element: <ClimateIntelJobsPage /> },
                { path: 'climate-intel/districts/:code', element: <DistrictRiskPage /> },
                { path: 'climate-intel/farms/:farmId', element: <FarmClimateIntelPage /> }
              ]
            },
            { path: 'weather', element: <Navigate to="/climate" replace /> },
            { path: 'settings', element: <SettingsPage /> },
            // ADR-010: retired from product surface (GIS/maps/guidance + partner-ops)
            { path: 'gis', element: <Navigate to="/climate-intel" replace /> },
            { path: 'guidance', element: <Navigate to="/planning" replace /> },
            { path: 'claims/*', element: <Navigate to="/planning" replace /> },
            { path: 'settlements/*', element: <Navigate to="/planning" replace /> },
            { path: 'policies/*', element: <Navigate to="/planning" replace /> },
            { path: 'insurance/*', element: <Navigate to="/planning" replace /> },
            { path: 'lending/*', element: <Navigate to="/planning" replace /> },
            { path: 'tasks/*', element: <Navigate to="/app" replace /> },
            { path: 'satellite', element: <Navigate to="/climate-intel" replace /> },
            { path: 'inputs', element: <Navigate to="/farmers" replace /> },
            { path: 'ledger/*', element: <Navigate to="/app" replace /> },
            { path: 'payment-providers', element: <Navigate to="/app" replace /> }
          ]
        }
      ]
    }
  ],
  {
    future: {
      v7_relativeSplatPath: true
    }
  }
);
