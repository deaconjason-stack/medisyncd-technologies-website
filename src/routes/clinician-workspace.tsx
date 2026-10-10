import { createFileRoute } from '@tanstack/react-router';
import { useState } from 'react';
import { ChevronRight, Check, RotateCcw, LockKeyhole } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { PageHeading, Status, SafetyNote } from '@/components/medisync/shared';
import { patients, pageHead } from '@/lib/demo-data';
import { useDemo } from '@/components/medisync/demo-context';
export const Route = createFileRoute('/clinician-workspace')({ head: () => pageHead('Clinician Workspace', 'Explore three fictional patient queue entries and complete or reopen mock encounters. No patient records or clinical documentation.'), component: ClinicianWorkspace });
function ClinicianWorkspace() {
  const [selected, setSelected] = useState('demo-001');
  const { completed, toggleEncounter } = useDemo();
  const patient = patients.find(p => p.id === selected) ?? patients[0];
  if (!patient) return <SafetyNote>No fictional queue entries available.</SafetyNote>;
  const isComplete = completed.includes(patient.id);
  return <><PageHeading eyebrow="MEDISYNCD TELEHEALTH PLATFORM" title="Clinician Workspace" description="A proposed care-team workflow with fictional people and mock encounters." action={<Status kind="demo">Demo workspace</Status>} />
    <div className="clinician-grid"><section><div className="section-heading"><h2>Fictional patient queue</h2><span>{3 - completed.length} pending</span></div><div className="queue-list">{patients.map(p => <Button variant="ghost" key={p.id} className={`queue-item ${selected === p.id ? 'selected' : ''}`} aria-pressed={selected === p.id} onClick={() => setSelected(p.id)}><span className="queue-avatar">{p.initials}</span><span className="queue-person"><strong>{p.name}</strong><small>{p.type}</small><small>{p.time} · {completed.includes(p.id) ? 'Mock complete' : 'Mock waiting'}</small></span><ChevronRight /></Button>)}</div><SafetyNote>All three people are invented. No patient information is collected or stored.</SafetyNote></section>
    <section className="encounter"><div className="encounter-top"><Status kind="neutral">Fictional encounter</Status><Status kind={isComplete ? 'complete' : 'pending'}>{isComplete ? 'Mock complete' : 'Mock waiting'}</Status></div><h2>{patient.name}</h2><p>Fictional person · {patient.id.toUpperCase()} · No patient record</p><dl className="detail-grid"><div><dt>Sample care type</dt><dd>{patient.type}</dd></div><div><dt>Simulated time</dt><dd>{patient.time} · Demo day 1</dd></div><div><dt>Sample duration</dt><dd>{patient.duration}</dd></div><div><dt>Encounter state</dt><dd aria-live="polite">{isComplete ? 'Mock completed' : 'Awaiting mock encounter'}</dd></div></dl><div className="encounter-summary"><h3>Mock encounter summary</h3><p>A fictional workflow placeholder only. There is no medical history, diagnosis, treatment plan, clinical note, or real encounter attached to this person.</p></div><div className="encounter-actions"><Button onClick={() => toggleEncounter(patient.id)}>{isComplete ? <RotateCcw /> : <Check />}{isComplete ? 'Reopen mock encounter' : 'Mark mock encounter complete'}</Button><Button variant="outline" disabled><LockKeyhole />Provide treatment — unavailable</Button></div><SafetyNote>Completing an encounter changes a sample status only. Zero live clinical encounters have occurred.</SafetyNote></section></div>
  </>;
}