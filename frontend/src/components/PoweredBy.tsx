export function PoweredBy({ className = '' }: { className?: string }) {
  return (
    <p className={`text-[11px] tracking-[0.06em] ${className}`}>
      <span className="opacity-75">Powered By</span> <span className="font-semibold">Cloud Sync</span>
    </p>
  );
}
