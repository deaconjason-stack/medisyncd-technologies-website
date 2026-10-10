import type { ReactNode } from 'react';
import { ArrowRight, CircleDashed, Info } from 'lucide-react';
import { Link } from '@tanstack/react-router';
import { Button } from '@/components/ui/button';
export function PageHeading({ eyebrow, title, description, action }: { eyebrow: string; title: string; description: string; action?: ReactNode }) { return <div className="page-heading"><div><div className="eyebrow">{eyebrow}</div><h1>{title}</h1><p>{description}</p></div>{action}</div>; }
export function Status({ children = 'Planned', kind = 'pending' }: { children?: ReactNode; kind?: 'pending' | 'demo' | 'complete' | 'neutral' }) { return <span className={`status-badge status-${kind}`}><span />{children}</span>; }
export function SafetyNote({ children }: { children: ReactNode }) { return <div className="safety-note"><Info size={17} /><span>{children}</span></div>; }
export function ReadinessLink() { return <Button asChild variant="outline"><Link to="/launch-readiness"><CircleDashed />Launch readiness<ArrowRight /></Link></Button>; }