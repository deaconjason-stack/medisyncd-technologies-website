import { createFileRoute, Link } from '@tanstack/react-router';
import { useState, type FormEvent } from 'react';
import { ArrowRight, Building2, CheckCircle2, ClipboardCopy, Clock3, HeartHandshake, LockKeyhole, Mail, MonitorPlay, ShieldAlert, Users } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { PageHeading, SafetyNote } from '@/components/medisync/shared';

export const Route = createFileRoute('/partners')({
  head: () => ({
    meta: [
      { title: 'Request a Practice Demo | MediSyncD Telehealth' },
      { name: 'description', content: 'Physicians, clinics and healthcare organizations: request a guided, fictional-data-only MediSyncD Telehealth demonstration. Business inquiries only.' },
      { name: 'robots', content: 'noindex, nofollow' }
    ]
  }),
  component: PartnersPage,
});

const INQUIRY_EMAIL = 'deaconjason@medisyncdtechnologies.com';
const practiceTypes = ['Physician practice', 'Outpatient clinic', 'Nursing or long-term care facility', 'Behavioral health organization', 'Hospital or health system', 'Other healthcare organization'];
const interests = ['Guided product demonstration', 'Discuss a potential design partnership', 'Explore future practice licensing', 'Discuss a future pilot (subject to readiness)'];
const timelines = ['As soon as convenient', 'Within the next month', 'Exploring options'];

function PartnersPage() {
  const [contactName, setContactName] = useState('');
  const [email, setEmail] = useState('');
  const [organization, setOrganization] = useState('');
  const [role, setRole] = useState('Practice owner / physician');
  const [practiceType, setPracticeType] = useState(practiceTypes[0]);
  const [interest, setInterest] = useState(interests[0]);
  const [timeline, setTimeline] = useState(timelines[0]);
  const [confirmed, setConfirmed] = useState(false);
  const [draftOpened, setDraftOpened] = useState(false);
  const [copyStatus, setCopyStatus] = useState<'idle' | 'copied' | 'manual'>('idle');

  const subject = 'MediSyncD Telehealth | Practice demo inquiry - ' + organization.trim();
  const message = [
    'Hello MediSyncD Technologies,',
    '',
    'I would like to request a discussion about the MediSyncD Telehealth demonstration.',
    '',
    'Contact name: ' + contactName.trim(),
    'Business email: ' + email.trim(),
    'Organization: ' + organization.trim(),
    'Role: ' + role,
    'Organization type: ' + practiceType,
    'Interest: ' + interest,
    'Preferred timeframe: ' + timeline,
    '',
    'I understand this request concerns a fictional-data-only product demonstration, not access to live medical services.',
    'No patient or protected health information is included.',
    '',
    'Please contact me to discuss next steps.',
  ].join('\n');
  const emailDraft = 'To: ' + INQUIRY_EMAIL + '\nSubject: ' + subject + '\n\n' + message;
  const mailto = 'mailto:' + INQUIRY_EMAIL + '?subject=' + encodeURIComponent(subject) + '&body=' + encodeURIComponent(message);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!event.currentTarget.reportValidity() || !confirmed) return;
    setDraftOpened(true);
    window.location.href = mailto;
  }
  async function copyDraft() {
    if (!contactName.trim() || !organization.trim() || !email.includes('@') || !confirmed) {
      setCopyStatus('manual');
      return;
    }
    try {
      await navigator.clipboard.writeText(emailDraft);
      setCopyStatus('copied');
    } catch {
      setCopyStatus('manual');
    }
  }

  return <>
    <PageHeading eyebrow="FOR HEALTHCARE ORGANIZATIONS" title="Let's explore better-connected care." description="We're inviting physicians, practice leaders, and healthcare organizations to experience an early MediSyncD Telehealth demonstration and share what matters most to their teams." />
    <section className="partner-hero">
      <div className="partner-hero-content">
        <span className="partner-kicker"><HeartHandshake size={16} /> EARLY PRACTICE CONVERSATIONS</span>
        <h2>See the vision. Help shape what comes next.</h2>
        <p>A guided walkthrough of the current prototype, followed by a discussion about your practice's workflows, priorities, and interest in future collaboration.</p>
        <div className="partner-hero-meta"><span><Clock3 size={16} /> Introductory conversation</span><span><ShieldAlert size={16} /> Fictional data only</span></div>
        <a className="partner-hero-button" href="#request-demo">Request a guided demo <ArrowRight size={17} /></a>
      </div>
      <div className="partner-visual" aria-hidden="true">
        <div className="partner-visual-window"><div className="partner-visual-dots"><i/><i/><i/></div><div className="partner-visual-top"><span/><span/></div><div className="partner-visual-row"><div/><div><span/><span/></div></div><div className="partner-visual-row"><div/><div><span/><span/></div></div><div className="partner-visual-row"><div/><div><span/><span/></div></div></div>
        <div className="partner-visual-chip"><CheckCircle2 size={17}/> Built for practical feedback</div>
      </div>
    </section>

    <section className="partner-section">
      <div className="partner-section-title"><span>WHAT YOU CAN EXPLORE</span><h2>Designed around the work of care.</h2><p>Current screens show proposed workflows, not operational clinical software.</p></div>
      <div className="partner-feature-grid">
        <article className="partner-feature"><span><MonitorPlay size={22}/></span><h3>Walk through the prototype</h3><p>Explore sample provider discovery, fictional appointments, visit-room visuals, and a mock clinician queue.</p></article>
        <article className="partner-feature"><span><Building2 size={22}/></span><h3>Discuss your practice's needs</h3><p>Tell us which administrative and care-coordination workflows your organization would want simplified.</p></article>
        <article className="partner-feature"><span><Users size={22}/></span><h3>Consider future collaboration</h3><p>Discuss design feedback, potential partnerships, and what would need to be validated before any clinical pilot.</p></article>
      </div>
    </section>

    <section className="partner-request-grid" id="request-demo">
      <div className="partner-request-explainer">
        <span className="partner-section-eyebrow">LET'S CONNECT</span>
        <h2>Request a practice demonstration.</h2>
        <p>Share basic business contact details and the topic you'd like to explore. We'll use the conversation to understand interest before building costly clinical infrastructure.</p>
        <div className="partner-process"><div><b>01</b><span><strong>Introduce your organization</strong><small>Only business contact details; no patient records.</small></span></div><div><b>02</b><span><strong>Arrange a guided conversation</strong><small>Review the working fictional-data-only demo.</small></span></div><div><b>03</b><span><strong>Discuss fit and next steps</strong><small>Any clinical pilot requires separate security, legal, and clinical approval.</small></span></div></div>
        <div className="partner-notice"><LockKeyhole size={20}/><div><strong>Business inquiries only</strong><p>Do not send patient names, diagnoses, medical records, or other sensitive health information. This is not a medical contact or emergency channel.</p></div></div>
      </div>
      <div className="partner-form-card">
        <div className="partner-form-heading"><Mail size={22}/><div><h3>Practice partnership inquiry</h3><p>No account, payment, or clinical information required.</p></div></div>
        <form onSubmit={handleSubmit} className="partner-form" autoComplete="on">
          <div className="partner-form-two">
            <label>Contact name <span>*</span><input required type="text" maxLength={100} autoComplete="name" placeholder="Your full name" value={contactName} onChange={e => setContactName(e.target.value)} /></label>
            <label>Work email <span>*</span><input required type="email" maxLength={180} autoComplete="email" placeholder="you@practice.com" value={email} onChange={e => setEmail(e.target.value)} /></label>
          </div>
          <label>Practice or organization <span>*</span><input required type="text" maxLength={140} autoComplete="organization" placeholder="Organization name" value={organization} onChange={e => setOrganization(e.target.value)} /></label>
          <div className="partner-form-two">
            <label>Your role<select value={role} onChange={e => setRole(e.target.value)}><option>Practice owner / physician</option><option>Medical director</option><option>Practice administrator</option><option>Nursing leader</option><option>IT / innovation leader</option><option>Other business representative</option></select></label>
            <label>Organization type<select value={practiceType} onChange={e => setPracticeType(e.target.value)}>{practiceTypes.map(item=><option key={item}>{item}</option>)}</select></label>
          </div>
          <label>What would you like to discuss?<select value={interest} onChange={e => setInterest(e.target.value)}>{interests.map(item=><option key={item}>{item}</option>)}</select></label>
          <label>Preferred timeframe<select value={timeline} onChange={e => setTimeline(e.target.value)}>{timelines.map(item=><option key={item}>{item}</option>)}</select></label>
          <label className="partner-confirm"><input type="checkbox" checked={confirmed} required onChange={e => setConfirmed(e.target.checked)} /><span>I understand this is a product demo inquiry only. I will not include patient or sensitive medical information.</span></label>
          <div className="partner-form-actions"><button className="partner-send" type="submit">Open my email draft <ArrowRight size={18}/></button><button className="partner-copy" type="button" onClick={copyDraft}><ClipboardCopy size={16}/> Copy inquiry</button></div>
          <p className="partner-form-note"><strong>How this works:</strong> This page opens a message in your own email application. The website does not send or store your details. <strong>You must press Send in your email app</strong> to complete your request.</p>
          {draftOpened && <p role="status" className="partner-form-feedback">Your email app should open with a prepared request. Please review and send it. Nothing has been submitted yet. If no app opens, use “Copy inquiry” and email <a href={'mailto:' + INQUIRY_EMAIL}>{INQUIRY_EMAIL}</a>.</p>}
          {copyStatus === 'copied' && <p role="status" className="partner-form-feedback">Inquiry text copied. Paste it into an email to <a href={'mailto:' + INQUIRY_EMAIL}>{INQUIRY_EMAIL}</a> and send it.</p>}
          {copyStatus === 'manual' && <p role="status" className="partner-form-feedback">Complete the required fields and confirmation first. If copying is unavailable on your device, use the email-draft button or contact <a href={'mailto:' + INQUIRY_EMAIL}>{INQUIRY_EMAIL}</a>.</p>}
        </form>
      </div>
    </section>

    <section className="partner-bottom-cta"><div><h2>Want to explore the platform first?</h2><p>Take a self-guided tour of the demonstration. No account or sensitive information required.</p></div><Button asChild variant="outline"><Link to="/clinician-workspace">Explore clinician workspace <ArrowRight size={16}/></Link></Button></section>
    <SafetyNote>All clinical workflows, appointments, and provider profiles elsewhere on this site are simulated. MediSyncD is not currently offering telemedicine medical services or live patient pilots.</SafetyNote>
  </>;
}
