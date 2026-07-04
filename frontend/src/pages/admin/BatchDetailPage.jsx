import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import api, { downloadFile, errorMessage } from '../../api/client';
import { Alert, Badge, Button, Card, CardBody, EmptyState, Field, Input, Select } from '../../components/ui';
import Spinner from '../../components/Spinner';

export default function BatchDetailPage() {
  const { batchId } = useParams();
  const navigate = useNavigate();

  const [batch, setBatch] = useState(null);
  const [enrolled, setEnrolled] = useState([]);
  const [allStudents, setAllStudents] = useState([]);
  const [notes, setNotes] = useState([]);
  const [tests, setTests] = useState([]);
  const [assignedFiles, setAssignedFiles] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  async function loadAll() {
    try {
      const [b, en, all, ns, ts, af] = await Promise.all([
        api.get(`/api/batches/${batchId}`),
        api.get(`/api/batches/${batchId}/students`),
        api.get('/api/students'),
        api.get(`/api/notes?batchId=${batchId}`),
        api.get(`/api/tests?batchId=${batchId}`),
        api.get(`/api/batches/${batchId}/library-files`),
      ]);
      setBatch(b.data);
      setEnrolled(en.data);
      setAllStudents(all.data);
      setNotes(ns.data);
      setTests(ts.data);
      setAssignedFiles(af.data);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadAll();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [batchId]);

  if (loading) return <Spinner full label="Loading batch..." />;
  if (!batch) return <Alert>{error || 'Batch not found.'}</Alert>;

  return (
    <div className="space-y-6">
      <div>
        <Link to="/admin/batches" className="text-sm text-brand hover:underline">
          ← Back to batches
        </Link>
        <h2 className="mt-1 text-xl font-semibold text-slate-800">{batch.name}</h2>
        {batch.description && <p className="text-sm text-slate-500">{batch.description}</p>}
      </div>

      {error && <Alert>{error}</Alert>}

      <StudentsSection
        batchId={batchId}
        enrolled={enrolled}
        allStudents={allStudents}
        onChange={loadAll}
        setError={setError}
      />

      <NotesSection batchId={batchId} notes={notes} onChange={loadAll} setError={setError} />

      <TestsSection batchId={batchId} tests={tests} onChange={loadAll} setError={setError} navigate={navigate} />

      <LibrarySection
        batchId={batchId}
        assignedFiles={assignedFiles}
        tests={tests}
        onChange={loadAll}
        setError={setError}
      />
    </div>
  );
}

function SectionHeader({ title, children }) {
  return (
    <div className="mb-3 flex items-center justify-between">
      <h3 className="text-lg font-semibold text-slate-800">{title}</h3>
      {children}
    </div>
  );
}

function StudentsSection({ batchId, enrolled, allStudents, onChange, setError }) {
  const [selectedId, setSelectedId] = useState('');
  const [creating, setCreating] = useState(false);
  const [newStudent, setNewStudent] = useState({ name: '', email: '', password: '' });
  const [busy, setBusy] = useState(false);

  const enrolledIds = new Set(enrolled.map((s) => s.id));
  const available = allStudents.filter((s) => !enrolledIds.has(s.id));

  async function enroll(e) {
    e.preventDefault();
    if (!selectedId) return;
    setBusy(true);
    try {
      await api.post(`/api/batches/${batchId}/students`, { studentId: Number(selectedId) });
      setSelectedId('');
      onChange();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function unenroll(studentId) {
    setBusy(true);
    try {
      await api.delete(`/api/batches/${batchId}/students/${studentId}`);
      onChange();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function createAndEnroll(e) {
    e.preventDefault();
    setBusy(true);
    try {
      const res = await api.post('/api/students', newStudent);
      await api.post(`/api/batches/${batchId}/students`, { studentId: res.data.id });
      setNewStudent({ name: '', email: '', password: '' });
      setCreating(false);
      onChange();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card>
      <CardBody>
        <SectionHeader title="Students">
          <Button variant="secondary" onClick={() => setCreating((c) => !c)}>
            {creating ? 'Cancel' : 'Add new student'}
          </Button>
        </SectionHeader>

        {creating && (
          <form onSubmit={createAndEnroll} className="mb-4 grid grid-cols-1 gap-3 rounded-lg bg-slate-50 p-3 sm:grid-cols-4">
            <Input
              placeholder="Name"
              value={newStudent.name}
              onChange={(e) => setNewStudent({ ...newStudent, name: e.target.value })}
              required
            />
            <Input
              type="email"
              placeholder="Email"
              value={newStudent.email}
              onChange={(e) => setNewStudent({ ...newStudent, email: e.target.value })}
              required
            />
            <Input
              type="password"
              placeholder="Password (min 8)"
              value={newStudent.password}
              onChange={(e) => setNewStudent({ ...newStudent, password: e.target.value })}
              required
            />
            <Button type="submit" disabled={busy}>
              Create &amp; enroll
            </Button>
          </form>
        )}

        <form onSubmit={enroll} className="mb-4 flex gap-2">
          <Select value={selectedId} onChange={(e) => setSelectedId(e.target.value)}>
            <option value="">Enroll an existing student…</option>
            {available.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name} ({s.email})
              </option>
            ))}
          </Select>
          <Button type="submit" disabled={busy || !selectedId}>
            Enroll
          </Button>
        </form>

        {enrolled.length === 0 ? (
          <p className="text-sm text-slate-500">No students enrolled yet.</p>
        ) : (
          <ul className="divide-y divide-slate-100">
            {enrolled.map((s) => (
              <li key={s.id} className="flex items-center justify-between py-2">
                <div>
                  <p className="text-sm font-medium text-slate-700">{s.name}</p>
                  <p className="text-xs text-slate-400">{s.email}</p>
                </div>
                <Button variant="ghost" onClick={() => unenroll(s.id)} disabled={busy}>
                  Remove
                </Button>
              </li>
            ))}
          </ul>
        )}
      </CardBody>
    </Card>
  );
}

function NotesSection({ batchId, notes, onChange, setError }) {
  const [subject, setSubject] = useState('');
  const [title, setTitle] = useState('');
  const fileRef = useRef(null);
  const [busy, setBusy] = useState(false);

  async function upload(e) {
    e.preventDefault();
    const file = fileRef.current?.files?.[0];
    if (!file) {
      setError('Please choose a PDF file.');
      return;
    }
    setBusy(true);
    try {
      const formData = new FormData();
      formData.append('batchId', batchId);
      formData.append('subject', subject);
      formData.append('title', title);
      formData.append('file', file);
      await api.post('/api/notes', formData);
      setSubject('');
      setTitle('');
      if (fileRef.current) fileRef.current.value = '';
      onChange();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function remove(id) {
    setBusy(true);
    try {
      await api.delete(`/api/notes/${id}`);
      onChange();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card>
      <CardBody>
        <SectionHeader title="Notes" />
        <form onSubmit={upload} className="mb-4 grid grid-cols-1 gap-3 rounded-lg bg-slate-50 p-3 sm:grid-cols-4">
          <Input placeholder="Subject" value={subject} onChange={(e) => setSubject(e.target.value)} required />
          <Input placeholder="Title" value={title} onChange={(e) => setTitle(e.target.value)} required />
          <input
            ref={fileRef}
            type="file"
            accept="application/pdf"
            className="text-sm text-slate-600 file:mr-3 file:rounded-lg file:border-0 file:bg-brand file:px-3 file:py-2 file:text-white"
            required
          />
          <Button type="submit" disabled={busy}>
            Upload PDF
          </Button>
        </form>

        {notes.length === 0 ? (
          <p className="text-sm text-slate-500">No notes uploaded yet.</p>
        ) : (
          <ul className="divide-y divide-slate-100">
            {notes.map((n) => (
              <li key={n.id} className="flex items-center justify-between py-2">
                <div>
                  <p className="text-sm font-medium text-slate-700">{n.title}</p>
                  <p className="text-xs text-slate-400">{n.subject}</p>
                </div>
                <div className="flex gap-1">
                  <Button variant="secondary" onClick={() => downloadFile(n.downloadUrl, `${n.title}.pdf`)}>
                    Download
                  </Button>
                  <Button variant="ghost" onClick={() => remove(n.id)} disabled={busy}>
                    Delete
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </CardBody>
    </Card>
  );
}

function TestsSection({ batchId, tests, onChange, setError, navigate }) {
  const [title, setTitle] = useState('');
  const [duration, setDuration] = useState(30);
  const [testType, setTestType] = useState('EXAM');
  const [negativeMarking, setNegativeMarking] = useState(false);
  const [negMark, setNegMark] = useState(0.25);
  const [busy, setBusy] = useState(false);

  async function create(e) {
    e.preventDefault();
    setBusy(true);
    try {
      const res = await api.post('/api/tests', {
        batchId: Number(batchId),
        title,
        durationMinutes: Number(duration),
        testType,
        negativeMarking,
        negativeMarkPerWrong: negativeMarking ? Number(negMark) : 0,
      });
      navigate(`/admin/tests/${res.data.id}`);
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  }

  async function unassignTest(testId) {
    setBusy(true);
    try {
      await api.delete(`/api/batches/${batchId}/library-tests/${testId}`);
      onChange();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card>
      <CardBody>
        <SectionHeader title="Tests" />
        <form onSubmit={create} className="mb-4 space-y-3 rounded-lg bg-slate-50 p-3">
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            <div className="sm:col-span-1">
              <Input placeholder="Test title" value={title} onChange={(e) => setTitle(e.target.value)} required />
            </div>
            <Input
              type="number"
              min="1"
              placeholder="Minutes"
              value={duration}
              onChange={(e) => setDuration(e.target.value)}
              required
            />
            <Select value={testType} onChange={(e) => setTestType(e.target.value)}>
              <option value="EXAM">Exam (one attempt)</option>
              <option value="PRACTICE">Practice (unlimited)</option>
            </Select>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            <label className="flex items-center gap-2 text-sm text-slate-600">
              <input
                type="checkbox"
                checked={negativeMarking}
                onChange={(e) => setNegativeMarking(e.target.checked)}
                className="accent-[color:var(--brand)]"
              />
              Negative marking
            </label>
            {negativeMarking && (
              <label className="flex items-center gap-2 text-sm text-slate-600">
                Deduct per wrong:
                <Input
                  type="number"
                  min="0"
                  step="0.25"
                  value={negMark}
                  onChange={(e) => setNegMark(e.target.value)}
                  className="w-24"
                />
              </label>
            )}
            <div className="ml-auto">
              <Button type="submit" disabled={busy}>
                Create &amp; add questions
              </Button>
            </div>
          </div>
        </form>

        {tests.length === 0 ? (
          <p className="text-sm text-slate-500">No tests created yet.</p>
        ) : (
          <ul className="divide-y divide-slate-100">
            {tests.map((t) => (
              <li key={t.id} className="flex items-center justify-between py-2">
                <div>
                  <p className="text-sm font-medium text-slate-700">{t.title}</p>
                  <p className="text-xs text-slate-400">
                    {t.durationMinutes} min · {t.questionCount} questions · {t.totalMarks} marks
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <Badge kind="blue">{t.testType === 'PRACTICE' ? 'Practice' : 'Exam'}</Badge>
                  {t.folderId && <Badge kind="slate">Library</Badge>}
                  {t.published ? <Badge kind="green">Published</Badge> : <Badge kind="amber">Draft</Badge>}
                  <Link to={`/admin/tests/${t.id}`}>
                    <Button variant="secondary">Manage</Button>
                  </Link>
                  {t.folderId && (
                    <Button variant="ghost" onClick={() => unassignTest(t.id)}>
                      Unassign
                    </Button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </CardBody>
    </Card>
  );
}

function LibrarySection({ batchId, assignedFiles, tests, onChange, setError }) {
  const [folders, setFolders] = useState([]);
  const [selectedFolderId, setSelectedFolderId] = useState('');
  const [folderDetail, setFolderDetail] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api
      .get('/api/library/folders')
      .then((res) => setFolders(res.data))
      .catch((err) => setError(errorMessage(err)));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!selectedFolderId) {
      setFolderDetail(null);
      return;
    }
    api
      .get(`/api/library/folders/${selectedFolderId}`)
      .then((res) => setFolderDetail(res.data))
      .catch((err) => setError(errorMessage(err)));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedFolderId]);

  const assignedFileIds = new Set(assignedFiles.map((f) => f.id));
  const assignedTestIds = new Set(tests.map((t) => t.id));

  async function run(fn) {
    setBusy(true);
    try {
      await fn();
      onChange();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card>
      <CardBody>
        <SectionHeader title="Library content" />

        {assignedFiles.length > 0 && (
          <div className="mb-4">
            <p className="mb-1 text-sm font-medium text-slate-600">Assigned files</p>
            <ul className="divide-y divide-slate-100">
              {assignedFiles.map((f) => (
                <li key={f.id} className="flex items-center justify-between py-2">
                  <div>
                    <p className="text-sm font-medium text-slate-700">{f.title}</p>
                    <p className="text-xs text-slate-400">{f.subject}</p>
                  </div>
                  <div className="flex gap-1">
                    <Button variant="secondary" onClick={() => downloadFile(f.downloadUrl, `${f.title}.pdf`)}>
                      Download
                    </Button>
                    <Button
                      variant="ghost"
                      disabled={busy}
                      onClick={() => run(() => api.delete(`/api/batches/${batchId}/library-files/${f.id}`))}
                    >
                      Unassign
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        )}

        <div className="rounded-lg bg-slate-50 p-3">
          <p className="mb-2 text-sm font-medium text-slate-600">Assign from a library folder</p>
          <Select value={selectedFolderId} onChange={(e) => setSelectedFolderId(e.target.value)}>
            <option value="">Choose a folder…</option>
            {folders.map((f) => (
              <option key={f.id} value={f.id}>
                {f.name}
              </option>
            ))}
          </Select>

          {folderDetail && (
            <div className="mt-3 space-y-3">
              <div>
                <p className="text-xs font-semibold uppercase text-slate-400">Files</p>
                {folderDetail.files.length === 0 ? (
                  <p className="text-sm text-slate-400">No files in this folder.</p>
                ) : (
                  folderDetail.files.map((f) => (
                    <div key={f.id} className="flex items-center justify-between py-1">
                      <span className="text-sm text-slate-700">
                        {f.title} <span className="text-slate-400">· {f.subject}</span>
                      </span>
                      {assignedFileIds.has(f.id) ? (
                        <Badge kind="green">Assigned</Badge>
                      ) : (
                        <Button
                          disabled={busy}
                          onClick={() => run(() => api.post(`/api/batches/${batchId}/library-files/${f.id}`))}
                        >
                          Assign
                        </Button>
                      )}
                    </div>
                  ))
                )}
              </div>
              <div>
                <p className="text-xs font-semibold uppercase text-slate-400">Tests</p>
                {folderDetail.tests.length === 0 ? (
                  <p className="text-sm text-slate-400">No tests in this folder.</p>
                ) : (
                  folderDetail.tests.map((t) => (
                    <div key={t.id} className="flex items-center justify-between py-1">
                      <span className="text-sm text-slate-700">
                        {t.title}{' '}
                        <span className="text-slate-400">
                          · {t.testType === 'PRACTICE' ? 'Practice' : 'Exam'} · {t.questionCount} q
                        </span>
                      </span>
                      {assignedTestIds.has(t.id) ? (
                        <Badge kind="green">Assigned</Badge>
                      ) : (
                        <Button
                          disabled={busy}
                          onClick={() => run(() => api.post(`/api/batches/${batchId}/library-tests/${t.id}`))}
                        >
                          Assign
                        </Button>
                      )}
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>
      </CardBody>
    </Card>
  );
}
