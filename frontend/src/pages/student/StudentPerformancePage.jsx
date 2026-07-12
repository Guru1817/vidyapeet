import { useEffect, useState } from 'react';
import api, { errorMessage } from '../../api/client';
import { Alert } from '../../components/ui';
import PerformanceView from '../../components/PerformanceView';
import Spinner from '../../components/Spinner';

export default function StudentPerformancePage() {
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get('/api/student/performance')
      .then((res) => setSummary(res.data))
      .catch((err) => setError(errorMessage(err)));
  }, []);

  if (error) return <Alert>{error}</Alert>;
  if (summary === null) return <Spinner full label="Loading performance..." />;

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-xl font-semibold text-slate-800 dark:text-slate-100">My performance</h2>
        <p className="text-sm text-slate-500 dark:text-slate-400">Your results across all submitted tests.</p>
      </div>
      <PerformanceView summary={summary} />
    </div>
  );
}
