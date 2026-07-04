import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { errorMessage } from '../../api/client';
import { formatScore } from '../../lib/format';
import { Alert, Badge, Button, Card, CardBody, EmptyState } from '../../components/ui';
import Spinner from '../../components/Spinner';

export default function StudentTestsPage() {
  const [tests, setTests] = useState(null);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    api
      .get('/api/student/tests')
      .then((res) => setTests(res.data))
      .catch((err) => setError(errorMessage(err)));
  }, []);

  if (tests === null) return <Spinner full label="Loading your tests..." />;

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-xl font-semibold text-slate-800">Mock tests</h2>
        <p className="text-sm text-slate-500">Tests assigned to your batches.</p>
      </div>

      {error && <Alert>{error}</Alert>}

      {tests.length === 0 ? (
        <EmptyState title="No tests available" hint="Your tests will appear here once your teacher publishes them." />
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {tests.map((t) => {
            const isPractice = t.testType === 'PRACTICE';
            const submitted = t.attemptStatus === 'SUBMITTED';
            const inProgress = t.attemptStatus === 'IN_PROGRESS';
            return (
              <Card key={t.testId}>
                <CardBody>
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className="font-medium text-slate-800">{t.title}</p>
                      <p className="mt-1 text-sm text-slate-500">
                        {t.durationMinutes} min · {t.questionCount} questions · {t.totalMarks} marks
                        {t.negativeMarking ? ' · negative marking' : ''}
                      </p>
                    </div>
                    <div className="flex shrink-0 flex-col items-end gap-1">
                      <Badge kind="blue">{isPractice ? 'Practice' : 'Exam'}</Badge>
                      {submitted && (
                        <Badge kind="green">
                          {isPractice ? 'Best ' : ''}
                          {formatScore(t.score)}/{t.totalMarks}
                        </Badge>
                      )}
                      {inProgress && <Badge kind="amber">In progress</Badge>}
                    </div>
                  </div>

                  <div className="mt-4 flex flex-wrap gap-2">
                    {submitted && (
                      <>
                        <Button variant="secondary" onClick={() => navigate(`/student/tests/${t.testId}/result`)}>
                          View result
                        </Button>
                        <Link to={`/student/tests/${t.testId}/leaderboard`}>
                          <Button variant="ghost">Leaderboard</Button>
                        </Link>
                        {isPractice && (
                          <Button onClick={() => navigate(`/student/tests/${t.testId}/take`)}>Retake</Button>
                        )}
                      </>
                    )}
                    {!submitted && (
                      <Button onClick={() => navigate(`/student/tests/${t.testId}/take`)}>
                        {inProgress ? 'Resume test' : 'Start test'}
                      </Button>
                    )}
                  </div>
                </CardBody>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
