import { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import PageHeader from '../../components/common/PageHeader';
import Tabs from '../../components/common/Tabs';
import CashJournalTab from './CashJournalTab';
import CashSessionTab from './CashSessionTab';
import ExpensesTab from './ExpensesTab';
import ReceivablesTab from './ReceivablesTab';
import PayablesTab from './PayablesTab';

export default function FinancePage() {
  const { can } = useAuth();
  const tabs = [
    can(['CASH_SESSION', 'FINANCE_VIEW']) && { value: 'session', label: 'Caisse du jour' },
    can('FINANCE_VIEW') && { value: 'journal', label: 'Journal de caisse' },
    can('FINANCE_VIEW') && { value: 'expenses', label: 'Dépenses' },
    can(['FINANCE_VIEW', 'SALES_VIEW']) && { value: 'receivables', label: 'Créances clients' },
    can(['FINANCE_VIEW', 'PURCHASE_VIEW']) && { value: 'payables', label: 'Dettes fournisseurs' },
  ].filter(Boolean);
  const [tab, setTab] = useState(tabs[0]?.value);

  return (
    <>
      <PageHeader title="Finance" description="Argent réellement encaissé et décaissé. Une vente à crédit ou une commande n'est pas un paiement." />
      <Tabs value={tab} onChange={setTab} tabs={tabs} />
      {tab === 'session' ? <CashSessionTab /> : null}
      {tab === 'journal' ? <CashJournalTab /> : null}
      {tab === 'expenses' ? <ExpensesTab /> : null}
      {tab === 'receivables' ? <ReceivablesTab /> : null}
      {tab === 'payables' ? <PayablesTab /> : null}
    </>
  );
}
