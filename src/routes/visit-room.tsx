import { createFileRoute, Link } from '@tanstack/react-router';
import { useState } from 'react';
import { Video, VideoOff, Mic, MicOff, PhoneOff, LockKeyhole, RotateCcw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { PageHeading, Status, SafetyNote } from '@/components/medisync/shared';
import { pageHead } from '@/lib/demo-data';
export const Route = createFileRoute('/visit-room')({ head: () => pageHead('Virtual Visit Room Preview', 'Visual preview only. Camera and microphone controls never access devices, and no live video or medical visit occurs.'), component: VisitRoom });
function VisitRoom() {
  const [camera, setCamera] = useState(false);
  const [mic, setMic] = useState(false);
  const [ended, setEnded] = useState(false);
  return <><PageHeading eyebrow="MEDISYNCD VIRTUAL CARE" title="Virtual Visit Room" description="A preview of a future visit experience. No live connection, devices, or medical services." action={<Status kind="demo">PREVIEW ONLY</Status>} />
    <div className="visit-meta"><Status kind="neutral">{ended ? 'Preview ended' : 'No live session'}</Status><span>Fictional participant · Dr. Avery Morgan</span></div>
    <section className="visit-stage" aria-label="Virtual visit visual preview"><div className="visit-stage-label"><LockKeyhole size={14} />VISUAL DEMONSTRATION ONLY</div><div className="visit-avatar">{ended ? <PhoneOff size={35} /> : 'AM'}</div><h2>{ended ? 'The preview has ended' : 'A connection, thoughtfully imagined.'}</h2><p>{ended ? 'No real call took place. You can restart the visual preview at any time.' : 'Dr. Avery Morgan is a fictional clinician. No video call is connected and no care is being provided.'}</p>{!ended && <div className="self-preview">{camera ? <Video size={22} /> : <VideoOff size={22} />}<span>Camera {camera ? 'on' : 'off'} · simulated</span></div>}</section>
    <div className="visit-controls">{ended ? <Button variant="outline" onClick={() => { setEnded(false); setCamera(false); setMic(false); }} className="!w-auto !rounded-md px-5"><RotateCcw />Restart preview</Button> : <><Button size="icon" variant={mic ? 'soft' : 'outline'} aria-label="Toggle simulated microphone" aria-pressed={mic} title="Simulated microphone — no device access" onClick={() => setMic(!mic)}>{mic ? <Mic /> : <MicOff />}</Button><Button size="icon" variant={camera ? 'soft' : 'outline'} aria-label="Toggle simulated camera" aria-pressed={camera} title="Simulated camera — no device access" onClick={() => setCamera(!camera)}>{camera ? <Video /> : <VideoOff />}</Button><Button size="icon" variant="destructive" aria-label="End visual preview" title="End preview" onClick={() => { setEnded(true); setCamera(false); setMic(false); }}><PhoneOff /></Button></>}</div>
    <p className="visit-control-note" role="status">{ended ? 'Preview ended. No call was connected.' : `Microphone ${mic ? 'on' : 'off'} · Camera ${camera ? 'on' : 'off'} — visual states only, devices never accessed.`}</p>
    <SafetyNote>No live video, audio, recording, chat, or clinical advice. These buttons change visual states only. A video vendor and applicable BAA remain pending.</SafetyNote>
    <div className="mt-6 flex justify-center"><Button variant="outline" asChild><Link to="/appointments">Back to sample appointments</Link></Button><Button disabled className="ml-3">Start real visit — unavailable</Button></div>
  </>;
}