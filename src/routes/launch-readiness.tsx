import { createFileRoute } from '@tanstack/react-router';
import { useState } from 'react';
import { ShieldCheck, CircleDashed, ChevronDown, Server, HeartPulse } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { PageHeading, Status, SafetyNote } from '@/components/medisync/shared';
import { safetyGates, pageHead } from '@/lib/demo-data';
export const Route = createFileRoute('/launch-readiness')({ head: () => pageHead('Launch Readiness', 'Fourteen pending clinical, legal, security, privacy, and operational safety gates. MediSyncD is not live and makes no compliance claims.'), component: LaunchReadiness });
function LaunchReadiness() {
  const [filter, setFilter] = useState('All safety gates');
  const filtered = safetyGates.filter(g => filter === 'All safety gates' || g.category === filter);
  return <><PageHeading eyebrow="SAFETY BEFORE LAUNCH" title="Launch Readiness" description="An honest view of what must be verified before either planned business line can launch." action={<Status>Not live · All gates pending</Status>} />
    <div className="readiness-summary"><div className="metric-card"><div className="metric-label">Verified safety gates<ShieldCheck size={16} /></div><div className="metric-value">0 / 14</div><p>No gate is represented as satisfied</p></div><div className="metric-card"><div className="metric-label">Live clinical encounters<HeartPulse size={16} /></div><div className="metric-value">0</div><p>No clinical service is active</p></div><div className="metric-card"><div className="metric-label">Backend connection<Server size={16} /></div><div className="metric-value">None</div><p>No patient data infrastructure</p></div></div>
    <div className="filter-toolbar"><div className="segmented-control" role="group" aria-label="Filter safety gates">{['All safety gates', 'Clinical & legal', 'Security & privacy', 'Operations'].map(category => <Button key={category} variant={filter === category ? 'soft' : 'ghost'} aria-pressed={filter === category} onClick={() => setFilter(category)}>{category}</Button>)}</div><span aria-live="polite">{filtered.length} pending gates</span></div>
    <div className="gate-list">{filtered.map(g => <details className="gate" key={g.title}><summary><CircleDashed /><span className="gate-title"><strong>{g.title}</strong><small>{g.category}</small></span><Status>Pending</Status><ChevronDown /></summary><p>{g.detail}</p></details>)}</div>
    <SafetyNote>These are pending review areas, not certifications or compliance claims. This demonstration does not establish clinical, legal, security, or operational readiness. Neither business line provides services or accepts real bookings.</SafetyNote>
  </>;
}