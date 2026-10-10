import { Link, useRouterState } from '@tanstack/react-router';
import { useState, type ReactNode } from 'react';
import { Activity, LayoutDashboard, Stethoscope, CalendarDays, Video, ClipboardList, Building2, ShieldCheck, HeartHandshake, ArrowUpRight, FlaskConical, Menu, X, ChevronDown, LockKeyhole } from 'lucide-react';
import { Button } from '@/components/ui/button';
const navigation = [
  { to: '/', label: 'Overview', icon: LayoutDashboard },
  { to: '/find-care', label: 'Find Care', icon: Stethoscope },
  { to: '/appointments', label: 'Patient Appointments', icon: CalendarDays },
  { to: '/visit-room', label: 'Virtual Visit Room', icon: Video },
  { to: '/clinician-workspace', label: 'Clinician Workspace', icon: ClipboardList },
  { to: '/practice-management', label: 'Practice Management', icon: Building2 },
  { to: '/partners', label: 'Request a Demo', icon: HeartHandshake },
  { to: '/launch-readiness', label: 'Launch Readiness', icon: ShieldCheck },
] as const;
export function AppShell({ children }: { children: ReactNode }) {
  const path = useRouterState({ select: s => s.location.pathname });
  const [mobileOpen, setMobileOpen] = useState(false);
  const current = navigation.find(item => item.to === path)?.label ?? 'Overview';
  return <div className="app-shell">
    <a href="#main-content" className="skip-link">Skip to content</a>
    {mobileOpen && <div className="sidebar-scrim" onClick={() => setMobileOpen(false)} aria-hidden="true" />}
    <aside className={`sidebar ${mobileOpen ? 'sidebar-open' : ''}`}>
      <Link to="/" className="brand" onClick={() => setMobileOpen(false)} aria-label="MediSyncD Telehealth overview"><span className="brand-mark"><Activity size={25} strokeWidth={2.4} /></span><span><strong>MediSyncD<span className="brand-dot">.</span></strong><small>TELEHEALTH</small></span></Link>
      <Button variant="sidebar" size="icon" className="mobile-close" onClick={() => setMobileOpen(false)} aria-label="Close navigation"><X /></Button>
      <div className="workspace-label">DEMONSTRATION WORKSPACE</div>
      <nav aria-label="Main navigation">{navigation.map((item, i) => <div key={item.to}>{i === 4 && <div className="nav-section">PROVIDER & ORGANIZATION</div>}<Button asChild variant="sidebar" className={`nav-link ${path === item.to ? 'nav-active' : ''}`}><Link to={item.to} onClick={() => setMobileOpen(false)} activeOptions={{ exact: true }}><item.icon size={18} /><span>{item.label}</span>{item.to === '/visit-room' && <span className="nav-preview">Preview</span>}</Link></Button></div>)}</nav>
      <div className="sidebar-bottom"><div className="sandbox-note"><div><FlaskConical size={18} /><strong>A safe space to explore</strong></div><p>Demo environment. No real care,<br />no patient data, no backend.</p><Link to="/partners">Request a practice demo <ArrowUpRight size={14} /></Link></div><div className="company-sidebar">MediSyncD Technologies, LLC<small>© 2026. All rights reserved.</small></div></div>
    </aside>
    <div className="main-shell"><header className="topbar"><div className="breadcrumb"><Button variant="ghost" size="icon" className="mobile-menu" onClick={() => setMobileOpen(true)} aria-label="Open navigation"><Menu /></Button><span className="breadcrumb-product">Workspace <span>/</span></span><strong>{current}</strong></div><div className="topbar-right"><Link className="partner-top-link" to="/partners">For practices <ArrowUpRight size={13}/></Link><span className="environment-pill"><span />Demo environment</span><span className="topbar-divider" /><span className="user-avatar">MD</span><span className="user-label">Demo explorer<small>No account required</small></span></div></header>
      <div className="demo-banner"><FlaskConical size={17} /><strong>DEMO ONLY</strong><span>Fictional data only — no medical services, real booking, diagnosis, or patient records.</span><LockKeyhole size={15} className="banner-lock" /></div>
      <main id="main-content" className="main-content">{children}</main>
      <footer className="page-footer"><span>© 2026 MediSyncD Technologies, LLC</span><span><span className="status-dot" /> Browser memory only <span className="footer-separator">·</span> Resets on refresh</span></footer>
    </div>
  </div>;
}