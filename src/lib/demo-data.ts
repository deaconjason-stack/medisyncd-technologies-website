export const providers = [
  { id: 'avery', name: 'Dr. Avery Morgan', initials: 'AM', specialty: 'Primary care', role: 'Fictional family physician', color: 'teal', slots: ['9:00 AM', '10:30 AM', '2:00 PM'] },
  { id: 'jordan', name: 'Dr. Jordan Ellis', initials: 'JE', specialty: 'Mental wellness', role: 'Fictional psychiatrist', color: 'blue', slots: ['9:30 AM', '11:00 AM', '3:00 PM'] },
  { id: 'riley', name: 'Dr. Riley Chen', initials: 'RC', specialty: 'Dermatology', role: 'Fictional dermatologist', color: 'rose', slots: ['10:00 AM', '1:00 PM', '3:30 PM'] },
] as const;
export type Booking = { id: string; providerId: string; time: string; day: string };
export const initialBookings: Booking[] = [{ id: 'sample-1', providerId: 'avery', time: '10:30 AM', day: 'Demo day 1' }];
export const patients = [
  { id: 'demo-001', name: 'Alex Sample', initials: 'AS', type: 'Primary care', time: '9:00 AM', duration: '20 minutes' },
  { id: 'demo-002', name: 'Taylor Example', initials: 'TE', type: 'Mental wellness', time: '9:30 AM', duration: '30 minutes' },
  { id: 'demo-003', name: 'Sam Fictional', initials: 'SF', type: 'Dermatology', time: '10:00 AM', duration: '20 minutes' },
];
export const safetyGates = [
  { title: 'Licensure', category: 'Clinical & legal', detail: 'Validate applicable professional and operational licensure before offering clinical services.' },
  { title: 'Clinician credentialing', category: 'Clinical & legal', detail: 'Establish identity, qualifications, credential verification, and ongoing review.' },
  { title: 'Legal structure', category: 'Clinical & legal', detail: 'Review entity structure, clinical responsibility, and applicable practice-of-medicine requirements.' },
  { title: 'Provider authorization by state', category: 'Clinical & legal', detail: 'Verify clinician authority in each patient location. No state availability is represented here.' },
  { title: 'HIPAA controls', category: 'Security & privacy', detail: 'Complete a risk assessment and implement administrative, physical, and technical safeguards.' },
  { title: 'Multi-factor authentication (MFA)', category: 'Security & privacy', detail: 'Implement and verify appropriate identity and multi-factor access controls.' },
  { title: 'Tenancy / row-level security (RLS)', category: 'Security & privacy', detail: 'Design and test tenant isolation and role-based data access. This demo has no database.' },
  { title: 'Secure medical data & retention', category: 'Security & privacy', detail: 'Establish encryption, secure storage, deletion, and documented retention policies.' },
  { title: 'Consent', category: 'Clinical & legal', detail: 'Create reviewed telehealth consent and authorization processes for each applicable jurisdiction.' },
  { title: 'Audit', category: 'Security & privacy', detail: 'Implement access and action logs, review processes, and tamper-resistant evidence.' },
  { title: 'Video visit vendor BAA', category: 'Operations', detail: 'Select a suitable video vendor and execute a business associate agreement where required.' },
  { title: 'Clinical safety', category: 'Clinical & legal', detail: 'Establish clinical protocols, escalation, emergency procedures, and quality oversight.' },
  { title: 'Billing', category: 'Operations', detail: 'Review payment, reimbursement, coding, and financial policies. No pricing or payments in this demo.' },
  { title: 'Incident response', category: 'Operations', detail: 'Define response ownership, notification obligations, exercises, and recovery procedures.' },
];
export function pageHead(title: string, description: string) {
  return { meta: [{ title: `${title} | MediSyncD Telehealth` }, { name: 'description', content: description }, { property: 'og:title', content: `${title} | MediSyncD Telehealth` }, { property: 'og:description', content: description }, { property: 'og:type', content: 'website' }, { name: 'twitter:card', content: 'summary_large_image' }] };
}