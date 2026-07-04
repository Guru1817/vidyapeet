import { useEffect, useState } from 'react';
import api, { errorMessage } from '../../api/client';
import { Alert, Badge, Button, Card, CardBody, EmptyState, Field, Input } from '../../components/ui';
import Spinner from '../../components/Spinner';

const EMPTY_FORM = {
  name: '',
  slug: '',
  primaryColor: '#2563EB',
  logoUrl: '',
  adminName: '',
  adminEmail: '',
  adminPassword: '',
};

export default function InstitutesPage() {
  const [institutes, setInstitutes] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null); // null => creating
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [saving, setSaving] = useState(false);

  const isEditing = editingId !== null;

  function load() {
    api
      .get('/api/institutes')
      .then((res) => setInstitutes(res.data))
      .catch((err) => setError(errorMessage(err)));
  }

  useEffect(load, []);

  function update(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  function openCreate() {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setError('');
    setNotice('');
    setShowForm(true);
  }

  function openEdit(inst) {
    setEditingId(inst.id);
    setForm({
      ...EMPTY_FORM,
      name: inst.name,
      slug: inst.slug,
      primaryColor: inst.primaryColor || '#2563EB',
      logoUrl: inst.logoUrl || '',
    });
    setError('');
    setNotice('');
    setShowForm(true);
  }

  function closeForm() {
    setShowForm(false);
    setEditingId(null);
    setForm(EMPTY_FORM);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setNotice('');
    setSaving(true);
    try {
      if (isEditing) {
        await api.put(`/api/institutes/${editingId}`, {
          name: form.name,
          logoUrl: form.logoUrl.trim() || null,
          primaryColor: form.primaryColor,
        });
        setNotice(`Updated "${form.name}".`);
      } else {
        const res = await api.post('/api/institutes', {
          ...form,
          logoUrl: form.logoUrl.trim() || null,
        });
        setNotice(`Created "${res.data.name}". Admin can sign in with institute code "${res.data.slug}".`);
      }
      closeForm();
      load();
    } catch (err) {
      setError(errorMessage(err, 'Could not save the institute.'));
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(inst) {
    const confirmed = window.confirm(
      `Delete "${inst.name}"? This permanently removes its students, batches, notes, tests and results. This cannot be undone.`,
    );
    if (!confirmed) return;
    setError('');
    setNotice('');
    try {
      await api.delete(`/api/institutes/${inst.id}`);
      setNotice(`Deleted "${inst.name}".`);
      load();
    } catch (err) {
      setError(errorMessage(err, 'Could not delete the institute.'));
    }
  }

  if (institutes === null) return <Spinner full label="Loading institutes..." />;

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-slate-800">Coaching centers</h2>
          <p className="text-sm text-slate-500">Create and manage tenant portals.</p>
        </div>
        <Button onClick={showForm ? closeForm : openCreate}>{showForm ? 'Cancel' : 'New institute'}</Button>
      </div>

      {notice && <Alert kind="success">{notice}</Alert>}
      {error && <Alert>{error}</Alert>}

      {showForm && (
        <Card>
          <CardBody>
            <h3 className="mb-3 text-lg font-semibold text-slate-800">
              {isEditing ? `Edit ${form.name}` : 'New institute'}
            </h3>
            <form onSubmit={handleSubmit} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="Institute name">
                <Input value={form.name} onChange={(e) => update('name', e.target.value)} required />
              </Field>
              <Field label="Slug (URL code)">
                <Input
                  value={form.slug}
                  onChange={(e) => update('slug', e.target.value.toLowerCase())}
                  placeholder="demo-classes"
                  required
                  disabled={isEditing}
                />
              </Field>
              <Field label="Primary color">
                <input
                  type="color"
                  value={form.primaryColor}
                  onChange={(e) => update('primaryColor', e.target.value)}
                  className="h-10 w-full rounded-lg border border-slate-300"
                />
              </Field>
              <Field label="Logo URL (optional)">
                <Input value={form.logoUrl} onChange={(e) => update('logoUrl', e.target.value)} />
              </Field>

              {!isEditing && (
                <>
                  <div className="sm:col-span-2 mt-2 border-t border-slate-100 pt-3">
                    <p className="text-sm font-medium text-slate-600">First admin account</p>
                  </div>
                  <Field label="Admin name">
                    <Input value={form.adminName} onChange={(e) => update('adminName', e.target.value)} required />
                  </Field>
                  <Field label="Admin email">
                    <Input
                      type="email"
                      value={form.adminEmail}
                      onChange={(e) => update('adminEmail', e.target.value)}
                      required
                    />
                  </Field>
                  <Field label="Admin password (min 8 chars)">
                    <Input
                      type="password"
                      value={form.adminPassword}
                      onChange={(e) => update('adminPassword', e.target.value)}
                      required
                    />
                  </Field>
                </>
              )}

              <div className="sm:col-span-2 flex gap-2">
                <Button type="submit" disabled={saving}>
                  {saving ? 'Saving...' : isEditing ? 'Save changes' : 'Create institute'}
                </Button>
                <Button type="button" variant="secondary" onClick={closeForm}>
                  Cancel
                </Button>
              </div>
            </form>
          </CardBody>
        </Card>
      )}

      {institutes.length === 0 ? (
        <EmptyState title="No institutes yet" hint="Create your first coaching center to get started." />
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {institutes.map((inst) => (
            <Card key={inst.id}>
              <CardBody>
                <div className="flex items-center gap-3">
                  {inst.logoUrl ? (
                    <img src={inst.logoUrl} alt={inst.name} className="h-8 w-8 rounded-md object-contain" />
                  ) : (
                    <span
                      className="h-8 w-8 rounded-md"
                      style={{ backgroundColor: inst.primaryColor || '#2563eb' }}
                    />
                  )}
                  <div>
                    <p className="font-medium text-slate-800">{inst.name}</p>
                    <Badge>{inst.slug}</Badge>
                  </div>
                </div>
                <div className="mt-4 flex gap-2">
                  <Button variant="secondary" onClick={() => openEdit(inst)}>
                    Edit
                  </Button>
                  <Button variant="danger" onClick={() => handleDelete(inst)}>
                    Delete
                  </Button>
                </div>
              </CardBody>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
