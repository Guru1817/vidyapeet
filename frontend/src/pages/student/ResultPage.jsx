import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client';
import { formatScore } from '../../lib/format';
import { Alert, Badge, Button, Card, CardBody } from '../../components/ui';
import Spinner from '../../components/Spinner';

const OPTS = ['A', 'B', 'C', 'D'];

export default function ResultPage() {
  const { testId } = useParams();
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get(`/api/student/tests/${testId}/result`)
      .then((res) => setResult(res.data))
      .catch((err) => setError(errorMessage(err)));
  }, [testId]);

  if (error) return <Alert>{error}</Alert>;
  if (result === null) return <Spinner full label="Loading result..." />;

  const pct = result.totalMarks > 0 ? Math.round((result.score / result.totalMarks) * 100) : 0;

  return (
    <div className="space-y-6">
      <Link to="/student/tests" className="text-sm text-brand hover:underline">
        ← Back to tests
      </Link>

      <Card>
        <CardBody className="flex flex-col items-center gap-2 text-center">
          <p className="text-sm text-slate-500">{result.title}</p>
          <div className="flex h-28 w-28 flex-col items-center justify-center rounded-full bg-brand/10">
            <span className="text-3xl font-bold text-brand">{formatScore(result.score)}</span>
            <span className="text-xs text-slate-500">of {result.totalMarks}</span>
          </div>
          <p className="text-lg font-semibold text-slate-800">{pct}%</p>
          <Link to={`/student/tests/${testId}/leaderboard`}>
            <Button variant="secondary">View leaderboard</Button>
          </Link>
        </CardBody>
      </Card>

      <div>
        <h3 className="mb-3 text-lg font-semibold text-slate-800">Answer breakdown</h3>
        <ol className="space-y-3">
          {result.breakdown.map((item, i) => (
            <Card key={item.questionId}>
              <CardBody>
                <div className="flex items-start justify-between gap-3">
                  <p className="font-medium text-slate-800">
                    {i + 1}. {item.text} <Badge>{item.type}</Badge>
                  </p>
                  {item.correct ? (
                    <Badge kind="green">+{formatScore(item.marksAwarded)}</Badge>
                  ) : (
                    <Badge kind="amber">{formatScore(item.marksAwarded)} / {item.marks}</Badge>
                  )}
                </div>
                <Breakdown item={item} />
                {!item.selectedAnswer && (
                  <p className="mt-2 text-xs text-slate-400">You did not answer this question.</p>
                )}
              </CardBody>
            </Card>
          ))}
        </ol>
      </div>
    </div>
  );
}

function Breakdown({ item }) {
  if (item.type === 'MCQ' || item.type === 'MSQ') {
    const correct = new Set((item.correctAnswer || '').split(',').filter(Boolean));
    const chosen = new Set((item.selectedAnswer || '').split(',').filter(Boolean));
    return (
      <ul className="mt-2 grid grid-cols-1 gap-1 text-sm sm:grid-cols-2">
        {OPTS.map((opt) => {
          const isCorrect = correct.has(opt);
          const isChosen = chosen.has(opt);
          let cls = 'px-2 py-1 text-slate-600';
          if (isCorrect) cls = 'rounded bg-green-50 px-2 py-1 font-medium text-green-700';
          else if (isChosen) cls = 'rounded bg-red-50 px-2 py-1 font-medium text-red-700';
          return (
            <li key={opt} className={cls}>
              {opt}. {item[`option${opt}`]}
              {isCorrect && ' ✓'}
              {isChosen && !isCorrect && ' ✗'}
            </li>
          );
        })}
      </ul>
    );
  }
  if (item.type === 'TRUE_FALSE') {
    return (
      <div className="mt-2 space-y-1 text-sm">
        <p className="text-green-700">Correct answer: {item.correctAnswer === 'TRUE' ? 'True' : 'False'}</p>
        {item.selectedAnswer && (
          <p className={item.correct ? 'text-green-700' : 'text-red-700'}>
            Your answer: {item.selectedAnswer === 'TRUE' ? 'True' : 'False'}
          </p>
        )}
      </div>
    );
  }
  // FILL_BLANK
  return (
    <div className="mt-2 space-y-1 text-sm">
      <p className="text-green-700">Accepted: {(item.correctAnswer || '').split('|').join(', ')}</p>
      {item.selectedAnswer && (
        <p className={item.correct ? 'text-green-700' : 'text-red-700'}>Your answer: {item.selectedAnswer}</p>
      )}
    </div>
  );
}
