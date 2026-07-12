import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import api, { downloadFile, errorMessage } from '../../api/client';
import { Alert, Badge, Button, Card, CardBody, Input, Select } from '../../components/ui';
import Spinner from '../../components/Spinner';

export default function LibraryFolderPage() {
  const { folderId } = useParams();
  const navigate = useNavigate();
  const [folder, setFolder] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  // file upload
  const [subject, setSubject] = useState('');
  const [title, setTitle] = useState('');
  const fileRef = useRef(null);

  // test create
  const [testTitle, setTestTitle] = useState('');
  const [duration, setDuration] = useState(30);
  const [testType, setTestType] = useState('EXAM');

  function load() {
    api
      .get(`/api/library/folders/${folderId}`)
      .then((res) => setFolder(res.data))
      .catch((err) => setError(errorMessage(err)));
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [folderId]);

  async function uploadFile(e) {
    e.preventDefault();
    const file = fileRef.current?.files?.[0];
    if (!file) {
      setError('Choose a PDF file.');
      return;
    }
    setBusy(true);
    try {
      const fd = new FormData();
      fd.append('subject', subject);
      fd.append('title', title);
      fd.append('file', file);
      await api.post(`/api/library/folders/${folderId}/files`, fd);
      setSubject('');
      setTitle('');
      if (fileRef.current) fileRef.current.value = '';
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function deleteFile(id) {
    setBusy(true);
    try {
      await api.delete(`/api/library/files/${id}`);
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function createTest(e) {
    e.preventDefault();
    setBusy(true);
    try {
      const res = await api.post('/api/tests', {
        folderId: Number(folderId),
        title: testTitle,
        durationMinutes: Number(duration),
        testType,
        negativeMarking: false,
      });
      navigate(`/admin/tests/${res.data.id}`);
    } catch (err) {
      setError(errorMessage(err));
      setBusy(false);
    }
  }

  async function deleteTest(id) {
    if (!window.confirm('Delete this library test? This also removes it from any batches and deletes its attempts.')) {
      return;
    }
    setBusy(true);
    try {
      await api.delete(`/api/tests/${id}`);
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  if (folder === null && !error) return <Spinner full label="Loading folder..." />;
  if (!folder) return <Alert>{error}</Alert>;

  return (
    <div className="space-y-6">
      <div>
        <Link to="/admin/library" className="text-sm text-brand hover:underline">
          ← Back to library
        </Link>
        <h2 className="mt-1 text-xl font-semibold text-slate-800 dark:text-slate-100">{folder.name}</h2>
        {folder.description && <p className="text-sm text-slate-500 dark:text-slate-400">{folder.description}</p>}
      </div>

      {error && <Alert>{error}</Alert>}

      {/* Files */}
      <Card>
        <CardBody>
          <h3 className="mb-3 text-lg font-semibold text-slate-800 dark:text-slate-100">Files</h3>
          <form onSubmit={uploadFile} className="mb-4 grid grid-cols-1 gap-3 rounded-lg bg-slate-50 dark:bg-slate-900/40 p-3 sm:grid-cols-4">
            <Input placeholder="Subject" value={subject} onChange={(e) => setSubject(e.target.value)} required />
            <Input placeholder="Title" value={title} onChange={(e) => setTitle(e.target.value)} required />
            <input
              ref={fileRef}
              type="file"
              accept="application/pdf"
              className="text-sm text-slate-600 dark:text-slate-300 file:mr-3 file:rounded-lg file:border-0 file:bg-brand file:px-3 file:py-2 file:text-white"
              required
            />
            <Button type="submit" disabled={busy}>
              Upload PDF
            </Button>
          </form>

          {folder.files.length === 0 ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">No files yet.</p>
          ) : (
            <ul className="divide-y divide-slate-100 dark:divide-slate-700">
              {folder.files.map((f) => (
                <li key={f.id} className="flex items-center justify-between py-2">
                  <div>
                    <p className="text-sm font-medium text-slate-700 dark:text-slate-200">{f.title}</p>
                    <p className="text-xs text-slate-400 dark:text-slate-500">{f.subject}</p>
                  </div>
                  <div className="flex gap-1">
                    <Button variant="secondary" onClick={() => downloadFile(f.downloadUrl, `${f.title}.pdf`)}>
                      Download
                    </Button>
                    <Button variant="ghost" onClick={() => deleteFile(f.id)} disabled={busy}>
                      Delete
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </CardBody>
      </Card>

      {/* Tests */}
      <Card>
        <CardBody>
          <h3 className="mb-3 text-lg font-semibold text-slate-800 dark:text-slate-100">Tests</h3>
          <form onSubmit={createTest} className="mb-4 grid grid-cols-1 gap-3 rounded-lg bg-slate-50 dark:bg-slate-900/40 p-3 sm:grid-cols-4">
            <div className="sm:col-span-2">
              <Input placeholder="Test title" value={testTitle} onChange={(e) => setTestTitle(e.target.value)} required />
            </div>
            <Select value={testType} onChange={(e) => setTestType(e.target.value)}>
              <option value="EXAM">Exam</option>
              <option value="PRACTICE">Practice</option>
            </Select>
            <Button type="submit" disabled={busy}>
              Create &amp; add questions
            </Button>
          </form>

          {folder.tests.length === 0 ? (
            <p className="text-sm text-slate-500 dark:text-slate-400">No tests yet.</p>
          ) : (
            <ul className="divide-y divide-slate-100 dark:divide-slate-700">
              {folder.tests.map((t) => (
                <li key={t.id} className="flex items-center justify-between py-2">
                  <div>
                    <p className="text-sm font-medium text-slate-700 dark:text-slate-200">{t.title}</p>
                    <p className="text-xs text-slate-400 dark:text-slate-500">
                      {t.durationMinutes} min · {t.questionCount} questions · {t.totalMarks} marks
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge kind="blue">{t.testType === 'PRACTICE' ? 'Practice' : 'Exam'}</Badge>
                    {t.published ? <Badge kind="green">Published</Badge> : <Badge kind="amber">Draft</Badge>}
                    <Link to={`/admin/tests/${t.id}`}>
                      <Button variant="secondary">Manage</Button>
                    </Link>
                    <Button variant="ghost" onClick={() => deleteTest(t.id)} disabled={busy}>
                      Delete
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </CardBody>
      </Card>
    </div>
  );
}
