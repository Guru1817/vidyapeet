export default function Spinner({ full = false, label }) {
  const spinner = (
    <div className="flex items-center gap-3 text-slate-500">
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-slate-300 border-t-brand" />
      {label && <span className="text-sm">{label}</span>}
    </div>
  );
  if (full) {
    return <div className="flex h-screen items-center justify-center">{spinner}</div>;
  }
  return <div className="flex justify-center py-8">{spinner}</div>;
}
