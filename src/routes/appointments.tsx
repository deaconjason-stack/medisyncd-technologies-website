import { createFileRoute, Link } from '@tanstack/react-router';
import { useState } from 'react';
import { CalendarDays, Video, Trash2, Clock, Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { PageHeading, Status, SafetyNote } from '@/components/medisync/shared';
import { providers, pageHead } from '@/lib/demo-data';
import { useDemo } from '@/components/medisync/demo-context';
export const Route = createFileRoute('/appointments')({ head: () => pageHead('Patient Appointments', 'View and remove fictional sample appointments in the MediSyncD browser-only demonstration.'), component: Appointments });
function Appointments() {
  const { bookings, removeBooking } = useDemo();
  const [removing, setRemoving] = useState<string | null>(null);
  const [notice, setNotice] = useState('');
  return <><PageHeading eyebrow="MEDISYNCD VIRTUAL CARE" title="Patient Appointments" description="Your sample bookings, all in one place. No real appointments are scheduled." action={<Button asChild><Link to="/find-care"><Plus />Add demo appointment</Link></Button>} />
    <div className="section-heading"><h2>Sample appointments</h2><span aria-live="polite">{bookings.length} demo {bookings.length === 1 ? 'booking' : 'bookings'}</span></div>
    <p role="status" className="mb-3 text-xs text-primary">{notice}</p>
    {bookings.length === 0 ? <div className="empty-state"><CalendarDays size={36} /><h2>No sample appointments</h2><p>Add a fictional booking to explore the appointment experience.</p><Button asChild><Link to="/find-care">Explore fictional clinicians</Link></Button></div> : <div className="appointment-list">{bookings.map(b => { const provider = providers.find(p => p.id === b.providerId); if (!provider) return null; return <article className="appointment-row" key={b.id}><div className="appointment-date"><span>DEMO DAY</span><strong>1</strong></div><div className="appointment-info"><Status kind="demo">Fictional booking</Status><h2>{provider.name}</h2><p>{provider.specialty} <span>·</span><Clock size={12} />{b.time} · Sample time</p></div><div className="appointment-actions"><Button variant="outline" asChild><Link to="/visit-room"><Video />Preview visit room</Link></Button><Button variant="ghost" size="icon" aria-label={`Remove sample booking with ${provider.name} at ${b.time}`} title="Remove sample booking" onClick={() => setRemoving(b.id)}><Trash2 /></Button></div></article>; })}</div>}
    <SafetyNote>These are fake appointments. No provider notification, real booking, payment, or patient record is created. Refreshing restores the original sample data.</SafetyNote>
    <Dialog open={!!removing} onOpenChange={open => { if (!open) setRemoving(null); }}><DialogContent className="max-w-[calc(100%-32px)] sm:max-w-lg"><DialogTitle>Remove this sample booking?</DialogTitle><DialogDescription>This only removes a fictional appointment from your current demo session. No real appointment is canceled.</DialogDescription><div className="confirmation-actions"><Button variant="destructive" onClick={() => { if (removing) removeBooking(removing); setRemoving(null); setNotice('Sample booking removed. No real appointment was canceled.'); }}>Remove demo booking</Button><Button variant="outline" onClick={() => setRemoving(null)}>Keep booking</Button></div></DialogContent></Dialog>
  </>;
}