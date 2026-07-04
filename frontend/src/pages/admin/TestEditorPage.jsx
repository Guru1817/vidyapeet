import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client';
import { Alert, Badge, Button, Card, CardBody, Field, Input, Select } from '../../components/ui';
import Spinner from '../../components/Spinner';

const OPTS = ['A', 'B', 'C', 'D'];

const EMPTY_Q = {
  type: 'MCQ',
  text: '',
  optionA: '',
  optionB: '',
  optionC: '',
  optionD: '',
  correctOption: 'A',
  correctOptions: [],
  correctBoolean: true,
  acceptedAnswers: '',
  marks: 1,
};

const TYPE_LABEL = {
  MCQ: 'Multiple choice (one answer)',
  MSQ: 'Multiple select (many answers)',
  TRUE_FALSE: 'True / False',
  FILL_BLANK: 'Fill in the blank',
};

function buildPayload(q) {
  const base = { type: q.type, text: q.text, marks: Number(q.marks) };
  switch (q.type) {
    case 'MCQ':
      return { ...base, optionA: q.optionA, optionB: q.optionB, optionC: q.optionC, optionD: q.optionD, correctOption: q.correctOption };
    case 'MSQ':
      return { ...base, optionA: q.optionA, optionB: q.optionB, optionC: q.optionC, optionD: q.optionD, correctOptions: q.correctOptions };
    case 'TRUE_FALSE':
      return { ...base, correctBoolean: q.correctBoolean };
    case 'FILL_BLANK':
      return {
        ...base,
        acceptedAnswers: q.acceptedAnswers.split(/[\n,]/).map((s) => s.trim()).filter(Boolean),
      };
    default:
      return base;
  }
}

export default function TestEditorPage() {
  const { testId } = useParams();
  const [test, setTest] = useState(null);
  const [title, setTitle] = useState('');
  const [duration, setDuration] = useState(30);
  const [testType, setTestType] = useState('EXAM');
  const [negativeMarking, setNegativeMarking] = useState(false);
  const [negMark, setNegMark] = useState(0.25);
  const [q, setQ] = useState(EMPTY_Q);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [busy, setBusy] = useState(false);
  const importRef = useRef(null);

  function load() {
    api
      .get(`/api/tests/${testId}`)
      .then((res) => {
        setTest(res.data);
        setTitle(res.data.title);
        setDuration(res.data.durationMinutes);
        setTestType(res.data.testType || 'EXAM');
        setNegativeMarking(res.data.negativeMarking);
        setNegMark(res.data.negativeMarkPerWrong || 0.25);
      })
      .catch((err) => setError(errorMessage(err)));
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [testId]);

  async function saveSettings(published) {
    setError('');
    setNotice('');
    setBusy(true);
    try {
      await api.put(`/api/tests/${testId}`, {
        title,
        durationMinutes: Number(duration),
        published,
        testType,
        negativeMarking,
        negativeMarkPerWrong: negativeMarking ? Number(negMark) : 0,
      });
      setNotice(published === test.published ? 'Saved.' : published ? 'Test published.' : 'Test unpublished.');
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function addQuestion(e) {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await api.post(`/api/tests/${testId}/questions`, buildPayload(q));
      setQ(EMPTY_Q);
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function deleteQuestion(questionId) {
    setBusy(true);
    try {
      await api.delete(`/api/tests/${testId}/questions/${questionId}`);
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function importExcel(e) {
    e.preventDefault();
    const file = importRef.current?.files?.[0];
    if (!file) return;
    setError('');
    setNotice('');
    setBusy(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      const res = await api.post(`/api/tests/${testId}/questions/import`, formData);
      setNotice(`Imported ${res.data.imported} questions.`);
      if (importRef.current) importRef.current.value = '';
      load();
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  function toggleMsqOption(opt) {
    setQ((cur) => {
      const set = new Set(cur.correctOptions);
      if (set.has(opt)) set.delete(opt);
      else set.add(opt);
      return { ...cur, correctOptions: Array.from(set) };
    });
  }

  if (test === null) return <Spinner full label="Loading test..." />;

  const needsOptions = q.type === 'MCQ' || q.type === 'MSQ';

  return (
    <div className="space-y-6">
      <div>
        <Link
          to={test.batchId ? `/admin/batches/${test.batchId}` : `/admin/library/folders/${test.folderId}`}
          className="text-sm text-brand hover:underline"
        >
          {test.batchId ? '← Back to batch' : '← Back to folder'}
        </Link>
        <div className="mt-1 flex items-center gap-3">
          <h2 className="text-xl font-semibold text-slate-800">{test.title}</h2>
          <Badge kind="blue">{test.testType === 'PRACTICE' ? 'Practice' : 'Exam'}</Badge>
          {test.published ? <Badge kind="green">Published</Badge> : <Badge kind="amber">Draft</Badge>}
        </div>
        <p className="text-sm text-slate-500">
          {test.questions.length} questions · {test.totalMarks} marks · {test.durationMinutes} min
          {test.negativeMarking ? ` · -${test.negativeMarkPerWrong} per wrong` : ''}
        </p>
      </div>

      {error && <Alert>{error}</Alert>}
      {notice && <Alert kind="success">{notice}</Alert>}

      {/* Settings */}
      <Card>
        <CardBody>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-4 sm:items-end">
            <div className="sm:col-span-2">
              <Field label="Title">
                <Input value={title} onChange={(e) => setTitle(e.target.value)} />
              </Field>
            </div>
            <Field label="Duration (minutes)">
              <Input type="number" min="1" value={duration} onChange={(e) => setDuration(e.target.value)} />
            </Field>
            <Field label="Type">
              <Select value={testType} onChange={(e) => setTestType(e.target.value)}>
                <option value="EXAM">Exam (one attempt)</option>
                <option value="PRACTICE">Practice (unlimited)</option>
              </Select>
            </Field>
          </div>
          <div className="mt-3 flex flex-wrap items-center gap-3">
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
            <div className="ml-auto flex gap-2">
              <Button variant="secondary" onClick={() => saveSettings(test.published)} disabled={busy}>
                Save
              </Button>
              <Button onClick={() => saveSettings(!test.published)} disabled={busy}>
                {test.published ? 'Unpublish' : 'Publish'}
              </Button>
            </div>
          </div>
          <div className="mt-3">
            <Link to={`/admin/tests/${test.id}/leaderboard`} className="text-sm text-brand hover:underline">
              View leaderboard →
            </Link>
          </div>
        </CardBody>
      </Card>

      {/* Bulk import */}
      <Card>
        <CardBody>
          <h3 className="mb-2 text-lg font-semibold text-slate-800">Bulk import (Excel)</h3>
          <p className="mb-3 text-sm text-slate-500">
            Columns: Question, Type (MCQ/MSQ/TRUE_FALSE/FILL_BLANK), Option A–D, Correct, Marks. Correct means:
            MCQ → <code>B</code>; MSQ → <code>A,C</code>; True/False → <code>TRUE</code>/<code>FALSE</code>;
            Fill → <code>newton|newtons</code>. The first row is a header.
          </p>
          <form onSubmit={importExcel} className="flex flex-wrap items-center gap-3">
            <input
              ref={importRef}
              type="file"
              accept=".xlsx,.xls"
              className="text-sm text-slate-600 file:mr-3 file:rounded-lg file:border-0 file:bg-brand file:px-3 file:py-2 file:text-white"
              required
            />
            <Button type="submit" disabled={busy}>
              Import
            </Button>
          </form>
        </CardBody>
      </Card>

      {/* Add question */}
      <Card>
        <CardBody>
          <h3 className="mb-3 text-lg font-semibold text-slate-800">Add a question</h3>
          <form onSubmit={addQuestion} className="space-y-3">
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3 sm:items-end">
              <Field label="Type">
                <Select value={q.type} onChange={(e) => setQ({ ...EMPTY_Q, type: e.target.value })}>
                  {Object.entries(TYPE_LABEL).map(([val, label]) => (
                    <option key={val} value={val}>
                      {label}
                    </option>
                  ))}
                </Select>
              </Field>
              <Field label="Marks">
                <Input type="number" min="1" value={q.marks} onChange={(e) => setQ({ ...q, marks: e.target.value })} />
              </Field>
            </div>

            <Field label={q.type === 'TRUE_FALSE' ? 'Statement' : 'Question'}>
              <Input value={q.text} onChange={(e) => setQ({ ...q, text: e.target.value })} required />
            </Field>

            {needsOptions && (
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                {OPTS.map((opt) => (
                  <div key={opt} className="flex items-center gap-2">
                    {q.type === 'MCQ' ? (
                      <input
                        type="radio"
                        name="correctOption"
                        checked={q.correctOption === opt}
                        onChange={() => setQ({ ...q, correctOption: opt })}
                        className="accent-[color:var(--brand)]"
                        title="Mark correct"
                      />
                    ) : (
                      <input
                        type="checkbox"
                        checked={q.correctOptions.includes(opt)}
                        onChange={() => toggleMsqOption(opt)}
                        className="accent-[color:var(--brand)]"
                        title="Mark correct"
                      />
                    )}
                    <span className="w-4 text-sm font-medium text-slate-500">{opt}</span>
                    <Input
                      value={q[`option${opt}`]}
                      onChange={(e) => setQ({ ...q, [`option${opt}`]: e.target.value })}
                      placeholder={`Option ${opt}`}
                      required
                    />
                  </div>
                ))}
                <p className="text-xs text-slate-400 sm:col-span-2">
                  {q.type === 'MCQ' ? 'Select the one correct option.' : 'Tick all correct options.'}
                </p>
              </div>
            )}

            {q.type === 'TRUE_FALSE' && (
              <Field label="Correct answer">
                <Select
                  value={q.correctBoolean ? 'TRUE' : 'FALSE'}
                  onChange={(e) => setQ({ ...q, correctBoolean: e.target.value === 'TRUE' })}
                >
                  <option value="TRUE">True</option>
                  <option value="FALSE">False</option>
                </Select>
              </Field>
            )}

            {q.type === 'FILL_BLANK' && (
              <Field label="Accepted answers (one per line; case-insensitive)">
                <textarea
                  rows={3}
                  value={q.acceptedAnswers}
                  onChange={(e) => setQ({ ...q, acceptedAnswers: e.target.value })}
                  placeholder={'newton\nnewtons'}
                  className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-2 focus:ring-brand/30"
                  required
                />
              </Field>
            )}

            <Button type="submit" disabled={busy}>
              Add question
            </Button>
          </form>
        </CardBody>
      </Card>

      {/* Questions list */}
      <Card>
        <CardBody>
          <h3 className="mb-3 text-lg font-semibold text-slate-800">Questions ({test.questions.length})</h3>
          {test.questions.length === 0 ? (
            <p className="text-sm text-slate-500">No questions yet. Add some above or import from Excel.</p>
          ) : (
            <ol className="space-y-3">
              {test.questions.map((question, i) => (
                <li key={question.id} className="rounded-lg border border-slate-100 p-3">
                  <div className="flex items-start justify-between gap-3">
                    <p className="font-medium text-slate-700">
                      {i + 1}. {question.text}{' '}
                      <Badge>{question.type}</Badge>{' '}
                      <span className="text-xs font-normal text-slate-400">({question.marks} marks)</span>
                    </p>
                    <Button variant="ghost" onClick={() => deleteQuestion(question.id)} disabled={busy}>
                      Delete
                    </Button>
                  </div>
                  <QuestionPreview question={question} />
                </li>
              ))}
            </ol>
          )}
        </CardBody>
      </Card>
    </div>
  );
}

function QuestionPreview({ question }) {
  if (question.type === 'MCQ' || question.type === 'MSQ') {
    const correct = new Set(
      question.type === 'MCQ' ? [question.correctOption] : question.correctOptions || [],
    );
    return (
      <ul className="mt-2 grid grid-cols-1 gap-1 text-sm sm:grid-cols-2">
        {OPTS.map((opt) => (
          <li
            key={opt}
            className={
              correct.has(opt)
                ? 'rounded bg-green-50 px-2 py-1 font-medium text-green-700'
                : 'px-2 py-1 text-slate-600'
            }
          >
            {opt}. {question[`option${opt}`]}
          </li>
        ))}
      </ul>
    );
  }
  if (question.type === 'TRUE_FALSE') {
    return <p className="mt-2 text-sm text-green-700">Answer: {question.correctBoolean ? 'True' : 'False'}</p>;
  }
  return (
    <p className="mt-2 text-sm text-green-700">
      Accepted: {(question.acceptedAnswers || []).join(', ')}
    </p>
  );
}
