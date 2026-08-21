import { createBrowserRouter } from 'react-router-dom';
import { ProtectedRoute } from '../auth/ProtectedRoute';
import AppLayout from '../layouts/AppLayout';
import ClaimsPage from '../pages/ClaimsPage';
import ClaimWizardPage from '../pages/ClaimWizardPage';
import ClaimDetailsPage from '../pages/ClaimDetailsPage';
import ClaimEvidencePage from '../pages/ClaimEvidencePage';
import ClaimAssessmentPage from '../pages/ClaimAssessmentPage';
import ClaimInspectionPage from '../pages/ClaimInspectionPage';
import CropHistoryPage from '../pages/CropHistoryPage';
import CropsPage from '../pages/CropsPage';
import DashboardPage from '../pages/DashboardPage';
import FarmBoundaryPage from '../pages/FarmBoundaryPage';
import FarmDetailsPage from '../pages/FarmDetailsPage';
import FarmPlotsPage from '../pages/FarmPlotsPage';
import FarmsPage from '../pages/FarmsPage';
import FarmerProfilePage from '../pages/FarmerProfilePage';
import FarmerRegistrationPage from '../pages/FarmerRegistrationPage';
import FarmersPage from '../pages/FarmersPage';
import ForgotPasswordPage from '../pages/ForgotPasswordPage';
import GISPage from '../pages/GISPage';
import HouseholdsPage from '../pages/HouseholdsPage';
import LoginPage from '../pages/LoginPage';
import SiteLayout from '../layouts/SiteLayout';
import HomePage from '../pages/public/HomePage';
import AboutPage from '../pages/public/AboutPage';
import PlatformPage from '../pages/public/PlatformPage';
import PartnersPage from '../pages/public/PartnersPage';
import JoinPage from '../pages/public/JoinPage';
import InsuranceProductsPage from '../pages/InsuranceProductsPage';
import InsuranceReportsPage from '../pages/InsuranceReportsPage';
import PoliciesPage from '../pages/PoliciesPage';
import PolicyDetailsPage from '../pages/PolicyDetailsPage';
import PolicyIssuancePage from '../pages/PolicyIssuancePage';
import PremiumCalculatorPage from '../pages/PremiumCalculatorPage';
import TasksPage from '../pages/TasksPage';
import TaskDetailsPage from '../pages/TaskDetailsPage';
import NotificationsPage from '../pages/NotificationsPage';
import NotificationDetailPage from '../pages/NotificationDetailPage';
import NotificationPreferencesPage from '../pages/NotificationPreferencesPage';
import ResetPasswordPage from '../pages/ResetPasswordPage';
import SeasonsPage from '../pages/SeasonsPage';
import SettingsPage from '../pages/SettingsPage';
import UsersPage from '../pages/UsersPage';
import SettlementDashboardPage from '../pages/SettlementDashboardPage';
import SettlementsPage from '../pages/SettlementsPage';
import SettlementDetailsPage from '../pages/SettlementDetailsPage';
import SettlementFinanceReviewPage from '../pages/SettlementFinanceReviewPage';
import SettlementReportsPage from '../pages/SettlementReportsPage';
import LedgerViewerPage from '../pages/LedgerViewerPage';
import PaymentProvidersPage from '../pages/PaymentProvidersPage';
import ClimateDashboardPage from '../pages/ClimateDashboardPage';
import ClimateStationsPage from '../pages/ClimateStationsPage';
import ClimateStationDetailPage from '../pages/ClimateStationDetailPage';
import ClimateObservationsPage from '../pages/ClimateObservationsPage';
import ClimateImportJobsPage from '../pages/ClimateImportJobsPage';
import ClimateDatasetsPage from '../pages/ClimateDatasetsPage';
import ClimateMapPage from '../pages/ClimateMapPage';
import NationalRiskDashboardPage from '../pages/NationalRiskDashboardPage';
import ClimateAlertsPage from '../pages/ClimateAlertsPage';
import FarmClimateIntelPage from '../pages/FarmClimateIntelPage';
import DistrictRiskPage from '../pages/DistrictRiskPage';
import ClimateIntelJobsPage from '../pages/ClimateIntelJobsPage';
import SatelliteIntelligencePage from '../pages/SatelliteIntelligencePage';
import LendingPage from '../pages/LendingPage';
import LoanDetailsPage from '../pages/LoanDetailsPage';
import InsuredInputsPage from '../pages/InsuredInputsPage';
import FarmerGuidancePage from '../pages/FarmerGuidancePage';
import { Navigate } from 'react-router-dom';

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
  {
    path: '/login',
    element: <LoginPage />
  },
  {
    path: '/forgot-password',
    element: <ForgotPasswordPage />
  },
  {
    path: '/reset-password',
    element: <ResetPasswordPage />
  },
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
              { path: 'farms/:id/boundary', element: <FarmBoundaryPage /> },
              { path: 'farms/:id/plots', element: <FarmPlotsPage /> },
              { path: 'farms/:id/crop-history', element: <CropHistoryPage /> },
              {
                element: <ProtectedRoute denyRoles={['FARMER']} />,
                children: [
                  { path: 'crops', element: <CropsPage /> },
                  { path: 'seasons', element: <SeasonsPage /> },
                  { path: 'gis', element: <GISPage /> }
                ]
              }
            ]
          },
          {
            element: <ProtectedRoute permission="tasks:read" />,
            children: [
              { path: 'tasks', element: <TasksPage /> },
              { path: 'tasks/:id', element: <TaskDetailsPage /> }
            ]
          },
          {
            element: <ProtectedRoute permission="notifications:read" />,
            children: [
              { path: 'notifications', element: <NotificationsPage /> },
              { path: 'notifications/:id', element: <NotificationDetailPage /> },
              { path: 'notification-preferences', element: <NotificationPreferencesPage /> }
            ]
          },
          {
            element: <ProtectedRoute permission="policies:read" />,
            children: [
              { path: 'policies', element: <PoliciesPage /> },
              { path: 'policies/:id', element: <PolicyDetailsPage /> },
              {
                element: <ProtectedRoute denyRoles={['FARMER']} />,
                children: [
                  { path: 'insurance/products', element: <InsuranceProductsPage /> },
                  { path: 'insurance/calculator', element: <PremiumCalculatorPage /> },
                  { path: 'insurance/reports', element: <InsuranceReportsPage /> }
                ]
              },
              {
                element: <ProtectedRoute permission="policies:write" />,
                children: [{ path: 'policies/issue', element: <PolicyIssuancePage /> }]
              }
            ]
          },
          {
            element: <ProtectedRoute permission="claims:read" />,
            children: [
              { path: 'claims', element: <ClaimsPage /> },
              {
                element: <ProtectedRoute permission="claims:write" />,
                children: [{ path: 'claims/new', element: <ClaimWizardPage /> }]
              },
              { path: 'claims/:id', element: <ClaimDetailsPage /> },
              { path: 'claims/:id/evidence', element: <ClaimEvidencePage /> },
              {
                element: <ProtectedRoute permission="claims:assess" />,
                children: [
                  { path: 'claims/:id/assessment', element: <ClaimAssessmentPage /> },
                  { path: 'claims/:id/inspection', element: <ClaimInspectionPage /> }
                ]
              }
            ]
          },
          {
            element: <ProtectedRoute permission="settlements:read" />,
            children: [
              { path: 'settlements', element: <SettlementsPage /> },
              { path: 'settlements/dashboard', element: <SettlementDashboardPage /> },
              {
                element: <ProtectedRoute permission="settlements:approve" />,
                children: [{ path: 'settlements/finance', element: <SettlementFinanceReviewPage /> }]
              },
              {
                element: <ProtectedRoute permission="reports:settlements" />,
                children: [{ path: 'settlements/reports', element: <SettlementReportsPage /> }]
              },
              { path: 'payment-providers', element: <PaymentProvidersPage /> },
              { path: 'settlements/:id', element: <SettlementDetailsPage /> }
            ]
          },
          {
            element: <ProtectedRoute permission="ledger:read" />,
            children: [
              { path: 'ledger', element: <LedgerViewerPage /> },
              { path: 'ledger/:id', element: <LedgerViewerPage /> }
            ]
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
              { path: 'climate/map', element: <ClimateMapPage /> }
            ]
          },
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
          {
            element: <ProtectedRoute permission="satellite:read" />,
            children: [{ path: 'satellite', element: <SatelliteIntelligencePage /> }]
          },
          {
            element: <ProtectedRoute permission="loans:read" />,
            children: [
              { path: 'lending', element: <LendingPage /> },
              { path: 'lending/:id', element: <LoanDetailsPage /> }
            ]
          },
          {
            element: <ProtectedRoute permission="inputs:read" />,
            children: [{ path: 'inputs', element: <InsuredInputsPage /> }]
          },
          { path: 'guidance', element: <FarmerGuidancePage /> },
          { path: 'weather', element: <Navigate to="/climate" replace /> },
          { path: 'settings', element: <SettingsPage /> }
        ]
      }
    ]
  }
  ],
  {
    future: {
      v7_startTransition: true,
      v7_relativeSplatPath: true
    }
  }
);
