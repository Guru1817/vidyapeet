import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../api/client';
import { Alert, Button, Card, CardBody, EmptyState } from '../../components/ui';
import Spinner from '../../components/Spinner';

export default function AdminPerformancePage() {
  const [rows, setRows] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get('/api/admin/performance')
      .then((res) => setRows(res.data))
      .catch((err) => setError(errorMessage(err)));
  }, []);

  if (error) return <Alert>{error}</Alert>;
  if (rows === null) return <Spinner full label="Loading performance..." />;

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-xl font-semibold text-slate-800 dark:text-slate-100">Student performance</h2>
        <p className="text-sm text-slate-500 dark:text-slate-400">Average scores across submitted tests. Click a student for details.</p>
      </div>

      {rows.length === 0 ? (
        <EmptyState title="No students yet" hint="Add students to track their performance." />
      ) : (
        <Card>
          <CardBody className="p-0">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-100 text-left text-xs uppercase text-slate-400 dark:border-slate-700 dark:text-slate-500">
                  <th className="px-5 py-3">Student</th>
                  <th className="px-5 py-3">Attempts</th>
                  <th className="px-5 py-3">Average</th>
                  <th className="px-5 py-3"></th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.studentId} className="border-b border-slate-50 last:border-0 dark:border-slate-700/60">
                    <td className="px-5 py-3">
                      <p className="font-medium text-slate-700 dark:text-slate-200">{r.name}</p>
                      <p className="text-xs text-slate-400 dark:text-slate-500">{r.email}</p>
                    </td>
                    <td className="px-5 py-3 text-slate-600 dark:text-slate-300">{r.totalAttempts}</td>
                    <td className="px-5 py-3 text-slate-600 dark:text-slate-300">{r.totalAttempts > 0 ? `${r.averagePercent}%` : '-'}</td>
                    <td className="px-5 py-3 text-right">
                      <Link to={`/admin/performance/${r.studentId}`}>
                        <Button variant="secondary">View</Button>
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardBody>
        </Card>
      )}
    </div>
  );
}
