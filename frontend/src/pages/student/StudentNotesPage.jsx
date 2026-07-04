import { useEffect, useState } from 'react';
import api, { downloadFile, errorMessage } from '../../api/client';
import { Alert, Badge, Button, Card, CardBody, EmptyState } from '../../components/ui';
import Spinner from '../../components/Spinner';

export default function StudentNotesPage() {
  const [notes, setNotes] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api
      .get('/api/student/notes')
      .then((res) => setNotes(res.data))
      .catch((err) => setError(errorMessage(err)));
  }, []);

  if (notes === null) return <Spinner full label="Loading notes..." />;

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-xl font-semibold text-slate-800">Study materials</h2>
        <p className="text-sm text-slate-500">Notes shared with your batches.</p>
      </div>

      {error && <Alert>{error}</Alert>}

      {notes.length === 0 ? (
        <EmptyState title="No notes yet" hint="Your teacher hasn't shared any materials yet." />
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {notes.map((n) => (
            <Card key={n.id}>
              <CardBody className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-slate-800">{n.title}</p>
                  <Badge>{n.subject}</Badge>
                </div>
                <Button variant="secondary" onClick={() => downloadFile(n.downloadUrl, `${n.title}.pdf`)}>
                  Download
                </Button>
              </CardBody>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
