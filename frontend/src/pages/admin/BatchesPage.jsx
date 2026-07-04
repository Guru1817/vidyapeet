import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../api/client';
import { Alert, Badge, Button, Card, CardBody, EmptyState, Field, Input, Textarea } from '../../components/ui';
import Spinner from '../../components/Spinner';

export default function BatchesPage() {
  const [batches, setBatches] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  function load() {
    api
      .get('/api/batches')
      .then((res) => setBatches(res.data))
      .catch((err) => setError(errorMessage(err)));
  }

  useEffect(load, []);

  async function handleCreate(e) {
    e.preventDefault();
    setError('');
    setSaving(true);
    try {
      await api.post('/api/batches', { name, description });
      setName('');
      setDescription('');
      setShowForm(false);
      load();
    } catch (err) {
      setError(errorMessage(err, 'Could not create the batch.'));
    } finally {
      setSaving(false);
    }
  }

  if (batches === null) return <Spinner full label="Loading batches..." />;

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-slate-800">Batches</h2>
          <p className="text-sm text-slate-500">Organize students, notes and tests by class.</p>
        </div>
        <Button onClick={() => setShowForm((s) => !s)}>{showForm ? 'Cancel' : 'New batch'}</Button>
      </div>

      {error && <Alert>{error}</Alert>}

      {showForm && (
        <Card>
          <CardBody>
            <form onSubmit={handleCreate} className="space-y-4">
              <Field label="Batch name">
                <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Class 10 - Science" required />
              </Field>
              <Field label="Description (optional)">
                <Textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={2} />
              </Field>
              <Button type="submit" disabled={saving}>
                {saving ? 'Creating...' : 'Create batch'}
              </Button>
            </form>
          </CardBody>
        </Card>
      )}

      {batches.length === 0 ? (
        <EmptyState title="No batches yet" hint="Create a batch to start adding students and tests." />
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {batches.map((b) => (
            <Link key={b.id} to={`/admin/batches/${b.id}`}>
              <Card className="h-full transition hover:shadow-md">
                <CardBody>
                  <p className="font-medium text-slate-800">{b.name}</p>
                  {b.description && <p className="mt-1 line-clamp-2 text-sm text-slate-500">{b.description}</p>}
                  <div className="mt-3">
                    <Badge kind="blue">{b.studentCount} students</Badge>
                  </div>
                </CardBody>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
