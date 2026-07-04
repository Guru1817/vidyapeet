import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client';
import { Alert } from '../../components/ui';
import PerformanceView from '../../components/PerformanceView';
import Spinner from '../../components/Spinner';

export default function AdminStudentPerformancePage() {
  const { studentId } = useParams();
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get(`/api/admin/students/${studentId}/performance`)
      .then((res) => setSummary(res.data))
      .catch((err) => setError(errorMessage(err)));
  }, [studentId]);

  if (error) return <Alert>{error}</Alert>;
  if (summary === null) return <Spinner full label="Loading performance..." />;

  return (
    <div className="space-y-5">
      <Link to="/admin/performance" className="text-sm text-brand hover:underline">
        ← Back to performance
      </Link>
      <PerformanceView summary={summary} />
    </div>
  );
}
