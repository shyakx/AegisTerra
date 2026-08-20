import type { WorkflowDecision } from '../api/decisions';

type Props = {
  decisions: WorkflowDecision[];
};

export default function DecisionTimeline({ decisions }: Props) {
  if (decisions.length === 0) {
    return <p className="text-sm text-textSecondary">No decisions recorded yet.</p>;
  }

  return (
    <ol className="relative space-y-4 border-l border-border pl-4">
      {decisions.map((d) => (
        <li key={d.id} className="relative">
          <span
            className="absolute -left-[1.3rem] top-1.5 h-2.5 w-2.5 rounded-full bg-primary"
            aria-hidden
          />
          <div className="rounded-xl border border-border bg-background px-3 py-2 text-sm">
            <div className="font-medium">
              {d.decisionTypeCode} → {d.outcomeCode}
            </div>
            <div className="text-xs text-textSecondary">
              {d.effect}
              {d.workflowAction ? ` · action ${d.workflowAction}` : ''} · step {d.stepCode}
            </div>
            {d.comment ? <p className="mt-1">{d.comment}</p> : null}
            <time className="mt-1 block text-xs text-textSecondary" dateTime={d.decidedAt}>
              {new Date(d.decidedAt).toLocaleString()}
            </time>
          </div>
        </li>
      ))}
    </ol>
  );
}
