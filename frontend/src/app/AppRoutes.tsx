import { lazy } from 'react';
import type { ReactNode } from 'react';
import { Navigate, Route, Routes, useParams } from 'react-router-dom';
import LazyRouteBoundary from '../components/LazyRouteBoundary';
import PropertyAppLayout from '../components/PropertyAppLayout';
import { useAuthentication } from '../hooks/useAuthentication';
import { isChecklistStage } from '../constants/checklist';
import type { PublicConfig } from '../types/PublicConfig';
import IntroPage from './IntroPage';
import LoginPage from './LoginPage';
import NotFoundPage from './NotFoundPage';
import ProtectedRoute from './ProtectedRoute';

const PropertyListPage = lazy(() => import('./PropertyListPage'));
const CreatePropertyPage = lazy(() => import('./CreatePropertyPage'));
const PropertyDetailPage = lazy(() => import('./PropertyDetailPage'));
const EditPropertyPage = lazy(() => import('./EditPropertyPage'));
const PropertyPhotosPage = lazy(() => import('./PropertyPhotosPage'));
const PropertyMemoPage = lazy(() => import('./PropertyMemoPage'));
const ChecklistListPage = lazy(() => import('./ChecklistListPage'));
const ChecklistHomePage = lazy(() => import('./ChecklistHomePage'));
const CreateChecklistPage = lazy(() => import('./CreateChecklistPage'));
const ChecklistDetailPage = lazy(() => import('./ChecklistDetailPage'));
const PropertyActiveChecklistPage = lazy(() => import('./PropertyActiveChecklistPage'));
const PropertyChecklistPage = lazy(() => import('./PropertyChecklistPage'));
const MyPage = lazy(() => import('./MyPage'));
const MapPage = lazy(() => import('./MapPage'));
const MapLocationSelectPage = lazy(() => import('./MapLocationSelectPage'));
const NearbyAnalysisPage = lazy(() => import('./NearbyAnalysisPage'));
const PropertyComparePage = lazy(() => import('./PropertyComparePage'));
const UpcomingFeaturePage = lazy(() => import('./UpcomingFeaturePage'));

const lazyPage = (page: ReactNode) => <LazyRouteBoundary>{page}</LazyRouteBoundary>;

const ChecklistResourceRoute = ({ config }: { config: PublicConfig }) => {
  const resource = useParams().resource;
  return isChecklistStage(resource) ? <ChecklistListPage config={config} /> : <ChecklistDetailPage config={config} />;
};

type AppRoutesProps = {
  config: PublicConfig;
};

const LoginRoute = ({ config }: AppRoutesProps) => {
  const { session } = useAuthentication();

  if (session !== null) {
    return <Navigate to="/properties" replace />;
  }

  return <LoginPage config={config} />;
};

const AppRoutes = ({ config }: AppRoutesProps) => (
  <Routes>
    <Route path="/intro" element={<IntroPage />} />
    <Route path="/login" element={<LoginRoute config={config} />} />
    <Route element={<ProtectedRoute config={config} />}>
      <Route element={<PropertyAppLayout />}>
        <Route index element={<Navigate to="/properties" replace />} />
        <Route path="/properties" element={lazyPage(<PropertyListPage config={config} />)} />
        <Route path="/properties/new" element={lazyPage(<CreatePropertyPage config={config} />)} />
        <Route path="/properties/:propertyId" element={lazyPage(<PropertyDetailPage config={config} />)} />
        <Route path="/properties/:propertyId/edit" element={lazyPage(<EditPropertyPage config={config} />)} />
        <Route path="/properties/:propertyId/photos" element={lazyPage(<PropertyPhotosPage config={config} />)} />
        <Route path="/properties/:propertyId/memo" element={lazyPage(<PropertyMemoPage config={config} />)} />
        <Route path="/properties/:propertyId/nearby" element={lazyPage(<NearbyAnalysisPage config={config} />)} />
        <Route
          path="/properties/:propertyId/active-checklists/:stage"
          element={lazyPage(<PropertyActiveChecklistPage config={config} />)}
        />
        <Route
          path="/properties/:propertyId/checklists/:propertyChecklistId"
          element={lazyPage(<PropertyChecklistPage config={config} />)}
        />
        <Route path="/checklists" element={lazyPage(<ChecklistHomePage />)} />
        <Route path="/checklists/new" element={lazyPage(<CreateChecklistPage config={config} />)} />
        <Route path="/checklists/:resource" element={lazyPage(<ChecklistResourceRoute config={config} />)} />
        <Route path="/me" element={lazyPage(<MyPage config={config} />)} />
        <Route path="/map" element={lazyPage(<MapPage config={config} />)} />
        <Route path="/map/select-location" element={lazyPage(<MapLocationSelectPage config={config} />)} />
        <Route path="/compare" element={lazyPage(<PropertyComparePage config={config} />)} />
        <Route path="/export" element={lazyPage(<UpcomingFeaturePage feature="export" />)} />
        <Route path="/tips" element={lazyPage(<UpcomingFeaturePage feature="tips" />)} />
      </Route>
    </Route>
    <Route path="*" element={<NotFoundPage />} />
  </Routes>
);

export default AppRoutes;
