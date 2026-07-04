import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api, { errorMessage } from '../../api/client';
import { useAuth } from '../../auth/AuthContext';
import { formatScore } from '../../lib/format';
import { Alert, Button, Card, CardBody, EmptyState } from '../../components/ui';
import Spinner from '../../components/Spinner';

const MEDAL = { 1: '🥇', 2: '🥈', 3: '🥉' };

export default function LeaderboardPage() {
  const { testId } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [entries, setEntries] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get(`/api/tests/${testId}/leaderboard`)
      .then((res) => setEntries(res.data))
      .catch((err) => setError(errorMessage(err)));
  }, [testId]);

  if (error) return <Alert>{error}</Alert>;
  if (entries === null) return <Spinner full label="Loading leaderboard..." />;

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-slate-800">Leaderboard</h2>
        <Button variant="ghost" onClick={() => navigate(-1)}>
          ← Back
        </Button>
      </div>

      {entries.length === 0 ? (
        <EmptyState title="No submissions yet" hint="Rankings appear once students submit this test." />
      ) : (
        <Card>
          <CardBody className="p-0">
            <ul className="divide-y divide-slate-100">
              {entries.map((e) => {
                const isMe = user?.role === 'STUDENT' && e.studentId === user.id;
                return (
                  <li
                    key={e.studentId}
                    className={`flex items-center justify-between px-5 py-3 ${isMe ? 'bg-brand/5' : ''}`}
                  >
                    <div className="flex items-center gap-3">
                      <span className="w-8 text-center text-lg font-semibold text-slate-500">
                        {MEDAL[e.rank] || e.rank}
                      </span>
                      <span className="font-medium text-slate-700">
                        {e.studentName}
                        {isMe && <span className="ml-2 text-xs text-brand">(you)</span>}
                      </span>
                    </div>
                    <span className="font-semibold text-slate-800">{formatScore(e.score)}</span>
                  </li>
                );
              })}
            </ul>
          </CardBody>
        </Card>
      )}
    </div>
  );
}
