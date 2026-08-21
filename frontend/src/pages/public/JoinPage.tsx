import { FormEvent, type ReactNode, useState } from 'react';
import { Link } from 'react-router-dom';
import { Reveal } from '../../motion/Reveal';
import { ConnectedStory } from '../../visuals/ConnectedStory';

const TYPES = [
  'Insurance company',
  'Financial institution',
  'Aggregator / input supplier',
  'Government institution',
  'Development partner',
  'Farmer organisation',
  'Other'
];

export default function JoinPage() {
  const [sent, setSent] = useState(false);
  const [org, setOrg] = useState('');
  const [type, setType] = useState(TYPES[0]);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [note, setNote] = useState('');

  function onSubmit(event: FormEvent) {
    event.preventDefault();
    setSent(true);
  }

  if (sent) {
    return (
      <div className="bg-background px-4 py-24 text-center">
        <Reveal y={24}>
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">Request received</p>
          <h1 className="font-display mt-3 text-3xl font-semibold">We will review your organisation.</h1>
          <p className="mx-auto mt-4 max-w-lg leading-relaxed text-textSecondary">
            Access is granted by mandate so each workspace only sees what it is allowed to own.
          </p>
          <Link to="/login" className="at-btn mt-8 inline-flex rounded-full bg-primary px-5 py-3 text-sm font-semibold text-white">
            Log in
          </Link>
        </Reveal>
      </div>
    );
  }

  return (
    <div className="bg-background">
      <div className="mx-auto grid max-w-6xl items-start gap-10 px-4 py-16 sm:px-6 lg:grid-cols-[1fr_0.9fr]">
        <Reveal x={-20} y={0}>
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary">Join</p>
          <h1 className="font-display mt-3 text-4xl font-semibold tracking-tight">Request a workspace</h1>
          <p className="mt-4 leading-relaxed text-textSecondary">
            Existing operators log in. This form is for institutions that need a new mandate on AegisTerra.
          </p>
          <form className="mt-10 space-y-4" onSubmit={onSubmit}>
            <Field label="Organisation">
              <input required value={org} onChange={(e) => setOrg(e.target.value)} className="w-full rounded-xl bg-surface px-4 py-3" />
            </Field>
            <Field label="Institution type">
              <select value={type} onChange={(e) => setType(e.target.value)} className="w-full rounded-xl bg-surface px-4 py-3">
                {TYPES.map((item) => (
                  <option key={item}>{item}</option>
                ))}
              </select>
            </Field>
            <Field label="Your name">
              <input required value={name} onChange={(e) => setName(e.target.value)} className="w-full rounded-xl bg-surface px-4 py-3" />
            </Field>
            <Field label="Work email">
              <input required type="email" value={email} onChange={(e) => setEmail(e.target.value)} className="w-full rounded-xl bg-surface px-4 py-3" />
            </Field>
            <Field label="What you need from the platform">
              <textarea required rows={4} value={note} onChange={(e) => setNote(e.target.value)} className="w-full rounded-xl bg-surface px-4 py-3" />
            </Field>
            <button type="submit" className="at-btn w-full rounded-full bg-primary py-3 text-sm font-semibold text-white">
              Submit request
            </button>
          </form>
        </Reveal>
        <Reveal x={24} y={0} className="hidden lg:block">
          <div className="sticky top-28">
            <ConnectedStory variant="action" />
          </div>
        </Reveal>
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block text-sm font-medium">
      <span className="mb-1.5 block">{label}</span>
      {children}
    </label>
  );
}
