type TimelineEvent = {
  id: string;
  eventType: string;
  fromStepCode?: string | null;
  toStepCode?: string | null;
  actionCode?: string | null;
  message?: string | null;
  occurredAt: string;
};

type Props = {
  events: TimelineEvent[];
  emptyMessage?: string;
};

export default function WorkflowTimeline({ events, emptyMessage = 'No timeline events yet.' }: Props) {
  if (!events.length) {
    return <p className="text-sm text-textSecondary">{emptyMessage}</p>;
  }

  return (
    <ol className="relative space-y-4 border-l border-border pl-6">
      {events.map((event) => (
        <li key={event.id} className="relative">
          <span className="absolute -left-[1.55rem] top-1 h-3 w-3 rounded-full bg-primary" aria-hidden />
          <p className="text-sm font-medium">{event.eventType}</p>
          <p className="text-xs text-textSecondary">{new Date(event.occurredAt).toLocaleString()}</p>
          <p className="mt-1 text-sm text-textSecondary">
            {[event.fromStepCode, event.toStepCode].filter(Boolean).join(' → ') || event.message || '—'}
            {event.actionCode ? ` · ${event.actionCode}` : ''}
          </p>
          {event.message ? <p className="mt-1 text-sm">{event.message}</p> : null}
        </li>
      ))}
    </ol>
  );
}
