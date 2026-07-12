import { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client';
import { useBranding } from '../../branding/BrandingContext';
import { Alert, Button, Card, CardBody } from '../../components/ui';
import Spinner from '../../components/Spinner';
import QuestionImage from '../../components/QuestionImage';

const OPTS = ['A', 'B', 'C', 'D'];

function formatTime(totalSeconds) {
  const s = Math.max(0, totalSeconds);
  const m = Math.floor(s / 60);
  const sec = s % 60;
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
}

// Group questions under their section labels. When there are no sections, a
// single ungrouped group (section: null) is returned so the test renders as a
// plain list. Questions whose sectionId does not match any section fall into a
// trailing ungrouped group.
function groupBySection(questions, sections) {
  const list = questions || [];
  const secs = sections || [];
  if (secs.length === 0) {
    return [{ section: null, questions: list }];
  }
  const ordered = [...secs].sort((a, b) => a.position - b.position);
  const known = new Set(ordered.map((s) => s.id));
  const groups = ordered.map((section) => ({
    section,
    questions: list.filter((q) => q.sectionId === section.id),
  }));
  const ungrouped = list.filter((q) => q.sectionId == null || !known.has(q.sectionId));
  if (ungrouped.length > 0) {
    groups.push({ section: null, questions: ungrouped });
  }
  return groups.filter((g) => g.questions.length > 0);
}

export default function TakeTestPage() {
  const { testId } = useParams();
  const navigate = useNavigate();
  const branding = useBranding();

  const [attempt, setAttempt] = useState(null);
  const [answers, setAnswers] = useState({}); // questionId -> canonical answer string
  const [secondsLeft, setSecondsLeft] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const submittingRef = useRef(false);

  const submit = useCallback(
    async (auto = false) => {
      if (submittingRef.current || !attempt) return;
      submittingRef.current = true;
      try {
        const payload = {
          answers: attempt.questions.map((q) => ({
            questionId: q.id,
            answer: answers[q.id] || null,
          })),
        };
        await api.post(`/api/student/attempts/${attempt.attemptId}/submit`, payload);
        navigate(`/student/tests/${testId}/result`, { replace: true });
      } catch (err) {
        setError(errorMessage(err, auto ? 'Auto-submit failed.' : 'Could not submit your test.'));
        submittingRef.current = false;
      }
    },
    [attempt, answers, navigate, testId],
  );

  useEffect(() => {
    let cancelled = false;
    api
      .post(`/api/student/tests/${testId}/start`)
      .then((res) => {
        if (cancelled) return;
        setAttempt(res.data);
        const remaining = Math.round((new Date(res.data.deadline).getTime() - Date.now()) / 1000);
        setSecondsLeft(remaining);
      })
      .catch((err) => {
        if (cancelled) return;
        if (err?.response?.status === 409) {
          navigate(`/student/tests/${testId}/result`, { replace: true });
          return;
        }
        setError(errorMessage(err));
      })
      .finally(() => !cancelled && setLoading(false));
    return () => {
      cancelled = true;
    };
  }, [testId, navigate]);

  useEffect(() => {
    if (secondsLeft === null) return undefined;
    if (secondsLeft <= 0) {
      submit(true);
      return undefined;
    }
    const id = setInterval(() => setSecondsLeft((s) => (s === null ? s : s - 1)), 1000);
    return () => clearInterval(id);
  }, [secondsLeft, submit]);

  function setSingle(qid, value) {
    setAnswers((a) => ({ ...a, [qid]: value }));
  }

  function toggleMulti(qid, opt) {
    setAnswers((a) => {
      const cur = a[qid] ? a[qid].split(',').filter(Boolean) : [];
      const set = new Set(cur);
      if (set.has(opt)) set.delete(opt);
      else set.add(opt);
      return { ...a, [qid]: Array.from(set).sort().join(',') };
    });
  }

  if (loading) return <Spinner full label="Preparing your test..." />;
  if (error && !attempt) {
    return (
      <div className="mx-auto max-w-lg p-6">
        <Alert>{error}</Alert>
        <Button className="mt-4" variant="secondary" onClick={() => navigate('/student/tests')}>
          Back to tests
        </Button>
      </div>
    );
  }
  if (!attempt) return null;

  const answeredCount = Object.values(answers).filter((v) => v && v.length > 0).length;
  const lowTime = secondsLeft !== null && secondsLeft <= 60;

  return (
    <div className="min-h-screen pb-28">
      <header className="sticky top-0 z-10 bg-brand text-white shadow">
        <div className="mx-auto flex max-w-3xl items-center justify-between px-4 py-3">
          <div>
            <p className="text-xs text-white/70">{branding.name}</p>
            <p className="font-semibold">{attempt.title}</p>
          </div>
          <div
            className={`rounded-lg px-3 py-1.5 font-mono text-lg font-semibold ${lowTime ? 'bg-red-600' : 'bg-white/15'}`}
            title="Time remaining"
          >
            {formatTime(secondsLeft ?? 0)}
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-3xl space-y-4 px-4 py-6">
        {error && <Alert>{error}</Alert>}

        {(() => {
          let n = 0;
          return groupBySection(attempt.questions, attempt.sections).map((group, gi) => (
            <section key={group.section?.id ?? `ungrouped-${gi}`} className="space-y-4">
              {group.section && (
                <h2 className="px-1 text-sm font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
                  {group.section.label}
                </h2>
              )}
              {group.questions.map((q) => {
                const num = (n += 1);
                return (
                  <Card key={q.id}>
                    <CardBody>
                      <p className="font-medium text-slate-800 dark:text-slate-100">
                        {num}. {q.text}{' '}
                        <span className="text-xs font-normal text-slate-400 dark:text-slate-500">({q.marks} marks)</span>
                      </p>
                      <QuestionImage questionId={q.id} imageKey={q.imageKey || q.image_key} />
                      <div className="mt-3">
                        <QuestionInput
                          question={q}
                          value={answers[q.id] || ''}
                          onSingle={(v) => setSingle(q.id, v)}
                          onToggle={(opt) => toggleMulti(q.id, opt)}
                        />
                      </div>
                    </CardBody>
                  </Card>
                );
              })}
            </section>
          ));
        })()}
      </main>

      <footer className="fixed bottom-0 left-0 right-0 border-t border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-800">
        <div className="mx-auto flex max-w-3xl items-center justify-between px-4 py-3">
          <span className="text-sm text-slate-500 dark:text-slate-400">
            Answered {answeredCount} of {attempt.questions.length}
          </span>
          <Button onClick={() => submit(false)} disabled={submittingRef.current}>
            Submit test
          </Button>
        </div>
      </footer>
    </div>
  );
}

function optionRow(selected, children) {
  return `flex cursor-pointer items-center gap-3 rounded-lg border px-3 py-2 text-sm transition ${
    selected
      ? 'border-brand bg-brand/5'
      : 'border-slate-200 hover:bg-slate-50 dark:border-slate-600 dark:hover:bg-slate-700/50'
  }`;
}

function QuestionInput({ question, value, onSingle, onToggle }) {
  if (question.type === 'MCQ') {
    return (
      <div className="space-y-2">
        {OPTS.map((opt) => (
          <label key={opt} className={optionRow(value === opt)}>
            <input
              type="radio"
              name={`q-${question.id}`}
              checked={value === opt}
              onChange={() => onSingle(opt)}
              className="accent-[color:var(--brand)]"
            />
            <span className="font-medium text-slate-500 dark:text-slate-400">{opt}.</span>
            <span className="text-slate-700 dark:text-slate-200">{question[`option${opt}`]}</span>
          </label>
        ))}
      </div>
    );
  }
  if (question.type === 'MSQ') {
    const selectedSet = new Set(value ? value.split(',') : []);
    return (
      <div className="space-y-2">
        <p className="text-xs text-slate-400 dark:text-slate-500">Select all that apply.</p>
        {OPTS.map((opt) => (
          <label key={opt} className={optionRow(selectedSet.has(opt))}>
            <input
              type="checkbox"
              checked={selectedSet.has(opt)}
              onChange={() => onToggle(opt)}
              className="accent-[color:var(--brand)]"
            />
            <span className="font-medium text-slate-500 dark:text-slate-400">{opt}.</span>
            <span className="text-slate-700 dark:text-slate-200">{question[`option${opt}`]}</span>
          </label>
        ))}
      </div>
    );
  }
  if (question.type === 'TRUE_FALSE') {
    return (
      <div className="space-y-2">
        {[
          ['TRUE', 'True'],
          ['FALSE', 'False'],
        ].map(([val, label]) => (
          <label key={val} className={optionRow(value === val)}>
            <input
              type="radio"
              name={`q-${question.id}`}
              checked={value === val}
              onChange={() => onSingle(val)}
              className="accent-[color:var(--brand)]"
            />
            <span className="text-slate-700 dark:text-slate-200">{label}</span>
          </label>
        ))}
      </div>
    );
  }
  // FILL_BLANK
  return (
    <input
      value={value}
      onChange={(e) => onSingle(e.target.value)}
      placeholder="Type your answer"
      className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-brand focus:ring-2 focus:ring-brand/30 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-500"
    />
  );
}
