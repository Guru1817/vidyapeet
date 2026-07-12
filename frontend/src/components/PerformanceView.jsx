import { formatScore } from '../lib/format';
import { Card, CardBody, EmptyState } from './ui';

function Stat({ label, value }) {
  return (
    <Card>
      <CardBody>
        <p className="text-3xl font-bold text-brand">{value}</p>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{label}</p>
      </CardBody>
    </Card>
  );
}

/** Renders a PerformanceSummary: headline stats + an attempts table. */
export default function PerformanceView({ summary }) {
  if (!summary || summary.totalAttempts === 0) {
    return <EmptyState title="No attempts yet" hint="Performance appears after the first submitted test." />;
  }

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Stat label="Tests attempted" value={summary.testsAttempted} />
        <Stat label="Total attempts" value={summary.totalAttempts} />
        <Stat label="Average score" value={`${summary.averagePercent}%`} />
        <Stat label="Best score" value={`${summary.bestPercent}%`} />
      </div>

      <Card>
        <CardBody className="p-0">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 text-left text-xs uppercase text-slate-400 dark:border-slate-700 dark:text-slate-500">
                <th className="px-5 py-3">Test</th>
                <th className="px-5 py-3">Score</th>
                <th className="px-5 py-3">%</th>
                <th className="px-5 py-3">Submitted</th>
              </tr>
            </thead>
            <tbody>
              {summary.attempts.map((a) => (
                <tr key={a.attemptId} className="border-b border-slate-50 last:border-0 dark:border-slate-700/60">
                  <td className="px-5 py-3 font-medium text-slate-700 dark:text-slate-200">{a.testTitle}</td>
                  <td className="px-5 py-3 text-slate-600 dark:text-slate-300">
                    {formatScore(a.score)} / {a.totalMarks}
                  </td>
                  <td className="px-5 py-3 text-slate-600 dark:text-slate-300">{a.percent}%</td>
                  <td className="px-5 py-3 text-slate-400 dark:text-slate-500">
                    {a.submittedAt ? new Date(a.submittedAt).toLocaleString() : '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </CardBody>
      </Card>
    </div>
  );
}
