import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../api/client';
import { useBranding } from '../../branding/BrandingContext';
import { Alert, Card, CardBody } from '../../components/ui';
import Spinner from '../../components/Spinner';

const TILES = [
  { key: 'students', label: 'Students', to: '/admin/students' },
  { key: 'batches', label: 'Batches', to: '/admin/batches' },
  { key: 'tests', label: 'Tests', to: '/admin/batches' },
  { key: 'notes', label: 'Notes', to: '/admin/batches' },
];

export default function AdminDashboardPage() {
  const branding = useBranding();
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get('/api/admin/dashboard')
      .then((res) => setStats(res.data))
      .catch((err) => setError(errorMessage(err)));
  }, []);

  if (error) return <Alert>{error}</Alert>;
  if (stats === null) return <Spinner full label="Loading dashboard..." />;

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-xl font-semibold text-slate-800 dark:text-slate-100">{branding.name}</h2>
        <p className="text-sm text-slate-500 dark:text-slate-400">Overview of your institute.</p>
      </div>

      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        {TILES.map((tile) => (
          <Link key={tile.key} to={tile.to}>
            <Card className="transition hover:shadow-md">
              <CardBody>
                <p className="text-3xl font-bold text-brand">{stats[tile.key]}</p>
                <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">{tile.label}</p>
              </CardBody>
            </Card>
          </Link>
        ))}
      </div>

      <Card>
        <CardBody>
          <h3 className="mb-2 text-lg font-semibold text-slate-800 dark:text-slate-100">Quick links</h3>
          <div className="flex flex-wrap gap-3 text-sm">
            <Link to="/admin/students" className="text-brand hover:underline">
              Manage students
            </Link>
            <Link to="/admin/batches" className="text-brand hover:underline">
              Batches, notes &amp; tests
            </Link>
          </div>
        </CardBody>
      </Card>
    </div>
  );
}
