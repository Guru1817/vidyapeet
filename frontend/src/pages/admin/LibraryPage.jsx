import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../../api/client';
import { Alert, Badge, Button, Card, CardBody, EmptyState, Field, Input, Textarea } from '../../components/ui';
import Spinner from '../../components/Spinner';

export default function LibraryPage() {
  const [folders, setFolders] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  function load() {
    api
      .get('/api/library/folders')
      .then((res) => setFolders(res.data))
      .catch((err) => setError(errorMessage(err)));
  }

  useEffect(load, []);

  async function create(e) {
    e.preventDefault();
    setError('');
    setSaving(true);
    try {
      await api.post('/api/library/folders', { name, description });
      setName('');
      setDescription('');
      setShowForm(false);
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  if (folders === null) return <Spinner full label="Loading library..." />;

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-slate-800">Library</h2>
          <p className="text-sm text-slate-500">Reusable folders of notes and tests you can share with batches.</p>
        </div>
        <Button onClick={() => setShowForm((s) => !s)}>{showForm ? 'Cancel' : 'New folder'}</Button>
      </div>

      {error && <Alert>{error}</Alert>}

      {showForm && (
        <Card>
          <CardBody>
            <form onSubmit={create} className="space-y-3">
              <Field label="Folder name">
                <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="Physics" required />
              </Field>
              <Field label="Description (optional)">
                <Textarea rows={2} value={description} onChange={(e) => setDescription(e.target.value)} />
              </Field>
              <Button type="submit" disabled={saving}>
                {saving ? 'Creating...' : 'Create folder'}
              </Button>
            </form>
          </CardBody>
        </Card>
      )}

      {folders.length === 0 ? (
        <EmptyState title="No folders yet" hint="Create a folder like 'Physics' to organize notes and tests." />
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {folders.map((f) => (
            <Link key={f.id} to={`/admin/library/folders/${f.id}`}>
              <Card className="h-full transition hover:shadow-md">
                <CardBody>
                  <p className="font-medium text-slate-800">{f.name}</p>
                  {f.description && <p className="mt-1 line-clamp-2 text-sm text-slate-500">{f.description}</p>}
                  <div className="mt-3 flex gap-2">
                    <Badge kind="blue">{f.fileCount} files</Badge>
                    <Badge kind="green">{f.testCount} tests</Badge>
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
