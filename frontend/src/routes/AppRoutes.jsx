import { Route, Routes } from 'react-router-dom';
import MainLayout from '../layouts/MainLayout';
import AuthLayout from '../layouts/AuthLayout';
import { RequireAuth, RequirePermission as P } from './ProtectedRoute';
import LoginPage from '../pages/auth/LoginPage';
import ChangePasswordPage from '../pages/auth/ChangePasswordPage';
import DashboardPage from '../pages/dashboard/DashboardPage';
import UsersPage from '../pages/users/UsersPage';
import ClientsPage from '../pages/clients/ClientsPage';
import SuppliersPage from '../pages/suppliers/SuppliersPage';
import CatalogPage from '../pages/catalog/CatalogPage';
import PurchasesPage from '../pages/purchases/PurchasesPage';
import ProductionPage from '../pages/production/ProductionPage';
import PressingPage from '../pages/pressing/PressingPage';
import StockPage from '../pages/stock/StockPage';
import PackagingPage from '../pages/packaging/PackagingPage';
import QuickSalePage from '../pages/sales/QuickSalePage';
import SalesHistoryPage from '../pages/sales/SalesHistoryPage';
import FinancePage from '../pages/finance/FinancePage';
import SettingsPage from '../pages/settings/SettingsPage';
import NotFoundPage from '../pages/errors/NotFoundPage';
import ReportsPage from '../pages/reports/ReportsPage';
import RolesPage from '../pages/admin/RolesPage';
import AuditPage from '../pages/admin/AuditPage';
import SalesDocsPage from '../pages/salesdocs/SalesDocsPage';
import QualityPage from '../pages/quality/QualityPage';
import HrPage from '../pages/hr/HrPage';
import NotificationsPage from '../pages/NotificationsPage';

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<AuthLayout />}>
        <Route path="/connexion" element={<LoginPage />} />
      </Route>

      <Route element={<RequireAuth />}>
        <Route element={<MainLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="profil" element={<ChangePasswordPage />} />
          <Route path="utilisateurs" element={<P permission="USER_MANAGE"><UsersPage /></P>} />
          <Route path="clients" element={<P permission="CLIENT_VIEW"><ClientsPage /></P>} />
          <Route path="fournisseurs" element={<P permission="SUPPLIER_VIEW"><SuppliersPage /></P>} />
          <Route path="catalogue" element={<P permission="CATALOG_VIEW"><CatalogPage /></P>} />
          <Route path="achats" element={<P permission="PURCHASE_VIEW"><PurchasesPage /></P>} />
          <Route path="production" element={<P permission="PRODUCTION_VIEW"><ProductionPage /></P>} />
          <Route path="pressage" element={<P permission="PRESSING_VIEW"><PressingPage /></P>} />
          <Route path="stock" element={<P permission="STOCK_VIEW"><StockPage /></P>} />
          <Route path="conditionnement" element={<P permission="PACKAGING_VIEW"><PackagingPage /></P>} />
          <Route path="ventes/rapide" element={<P permission="SALES_CREATE"><QuickSalePage /></P>} />
          <Route path="ventes/historique" element={<P permission="SALES_VIEW"><SalesHistoryPage /></P>} />
          <Route path="ventes/devis" element={<P permission="SALES_VIEW"><SalesDocsPage key="DEVIS" type="DEVIS" /></P>} />
          <Route path="ventes/commandes" element={<P permission="SALES_VIEW"><SalesDocsPage key="COMMANDE" type="COMMANDE" /></P>} />
          <Route path="ventes/livraisons" element={<P permission="SALES_VIEW"><SalesDocsPage key="LIVRAISON" type="LIVRAISON" /></P>} />
          <Route path="ventes/factures" element={<P permission="SALES_VIEW"><SalesDocsPage key="FACTURE" type="FACTURE" /></P>} />
          <Route path="ventes/avoirs" element={<P permission="SALES_VIEW"><SalesDocsPage key="AVOIR" type="AVOIR" /></P>} />
          <Route path="qualite" element={<P permission="QUALITY_VIEW"><QualityPage /></P>} />
          <Route path="rh" element={<P permission={['HR_VIEW', 'LEAVE_REQUEST', 'LEAVE_VALIDATE']}><HrPage /></P>} />
          <Route path="notifications" element={<NotificationsPage />} />
          <Route path="finance" element={<P permission={['FINANCE_VIEW', 'CASH_SESSION']}><FinancePage /></P>} />
          <Route path="rapports" element={<P permission="REPORT_VIEW"><ReportsPage /></P>} />
          <Route path="roles" element={<P permission={['ROLE_MANAGE', 'USER_MANAGE']}><RolesPage /></P>} />
          <Route path="journal" element={<P permission="AUDIT_VIEW"><AuditPage /></P>} />
          <Route path="parametres" element={<P permission={['SETTINGS_MANAGE', 'CATALOG_MANAGE']}><SettingsPage /></P>} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Route>
    </Routes>
  );
}
