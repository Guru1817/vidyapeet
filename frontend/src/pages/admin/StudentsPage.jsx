import { useEffect, useState } from 'react';
import api, { errorMessage } from '../../api/client';
import { Alert, Button, Card, CardBody, EmptyState, Field, Input, Textarea } from '../../components/ui';
import Spinner from '../../components/Spinner';

const EMPTY = { name: '', email: '', password: '', description: '' };

export default function StudentsPage() {
  const [students, setStudents] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [saving, setSaving] = useState(false);

  const isEditing = editingId !== null;

  function load() {
    api
      .get('/api/students')
      .then((res) => setStudents(res.data))
      .catch((err) => setError(errorMessage(err)));
  }

  useEffect(load, []);

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  function openCreate() {
    setEditingId(null);
    setForm(EMPTY);
    setError('');
    setNotice('');
    setShowForm(true);
  }

  function openEdit(s) {
    setEditingId(s.id);
    setForm({ name: s.name, email: s.email, password: '', description: s.description || '' });
    setError('');
    setNotice('');
    setShowForm(true);
  }

  function close() {
    setShowForm(false);
    setEditingId(null);
    setForm(EMPTY);
  }

  async function submit(e) {
    e.preventDefault();
    setError('');
    setNotice('');
    setSaving(true);
    try {
      if (isEditing) {
        const payload = {
          name: form.name,
          email: form.email,
          description: form.description || null,
        };
        if (form.password.trim()) payload.password = form.password.trim();
        await api.put(`/api/students/${editingId}`, payload);
        setNotice('Student updated.');
      } else {
        await api.post('/api/students', { ...form, description: form.description || null });
        setNotice('Student created.');
      }
      close();
      load();
    } catch (err) {
      setError(errorMessage(err, 'Could not save the student.'));
    } finally {
      setSaving(false);
    }
  }

  async function remove(s) {
    if (!window.confirm(`Delete ${s.name}? This removes their enrollments and test attempts. This cannot be undone.`)) {
      return;
    }
    setError('');
    setNotice('');
    try {
      await api.delete(`/api/students/${s.id}`);
      setNotice(`Deleted ${s.name}.`);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  if (students === null) return <Spinner full label="Loading students..." />;

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-slate-800">Students</h2>
          <p className="text-sm text-slate-500">All students in your institute.</p>
        </div>
        <Button onClick={showForm ? close : openCreate}>{showForm ? 'Cancel' : 'Add student'}</Button>
      </div>

      {notice && <Alert kind="success">{notice}</Alert>}
      {error && <Alert>{error}</Alert>}

      {showForm && (
        <Card>
          <CardBody>
            <h3 className="mb-3 text-lg font-semibold text-slate-800">
              {isEditing ? `Edit ${form.name}` : 'New student'}
            </h3>
            <form onSubmit={submit} className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="Name">
                <Input value={form.name} onChange={(e) => set('name', e.target.value)} required />
              </Field>
              <Field label="Email">
                <Input type="email" value={form.email} onChange={(e) => set('email', e.target.value)} required />
              </Field>
              <Field label={isEditing ? 'New password (leave blank to keep)' : 'Password (min 8 chars)'}>
                <Input
                  type="password"
                  value={form.password}
                  onChange={(e) => set('password', e.target.value)}
                  required={!isEditing}
                />
              </Field>
              <div className="sm:col-span-2">
                <Field label="Description (optional)">
                  <Textarea
                    rows={2}
                    value={form.description}
                    onChange={(e) => set('description', e.target.value)}
                    placeholder="Any notes about this student (guardian, class, etc.)"
                  />
                </Field>
              </div>
              <div className="sm:col-span-2 flex gap-2">
                <Button type="submit" disabled={saving}>
                  {saving ? 'Saving...' : isEditing ? 'Save changes' : 'Create student'}
                </Button>
                <Button type="button" variant="secondary" onClick={close}>
                  Cancel
                </Button>
              </div>
            </form>
          </CardBody>
        </Card>
      )}

      {students.length === 0 ? (
        <EmptyState title="No students yet" hint="Add your first student to get started." />
      ) : (
        <Card>
          <CardBody className="p-0">
            <ul className="divide-y divide-slate-100">
              {students.map((s) => (
                <li key={s.id} className="flex items-start justify-between gap-3 px-5 py-3">
                  <div>
                    <p className="font-medium text-slate-800">{s.name}</p>
                    <p className="text-sm text-slate-400">{s.email}</p>
                    {s.description && <p className="mt-1 text-sm text-slate-500">{s.description}</p>}
                  </div>
                  <div className="flex shrink-0 gap-1">
                    <Button variant="secondary" onClick={() => openEdit(s)}>
                      Edit
                    </Button>
                    <Button variant="ghost" onClick={() => remove(s)}>
                      Delete
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          </CardBody>
        </Card>
      )}
    </div>
  );
}
