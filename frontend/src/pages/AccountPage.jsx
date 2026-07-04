import { useState } from 'react';
import { useAuth } from '../auth/AuthContext';
import { errorMessage } from '../api/client';
import { Alert, Button, Card, CardBody, Field, Input } from '../components/ui';

export default function AccountPage() {
  const { user, updateCredentials } = useAuth();

  const [currentPassword, setCurrentPassword] = useState('');
  const [newEmail, setNewEmail] = useState(user?.email || '');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const emailChanged = newEmail.trim() && newEmail.trim() !== (user?.email || '');
  const passwordChanged = newPassword.length > 0;

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setNotice('');

    if (!currentPassword) {
      setError('Enter your current password to confirm changes.');
      return;
    }
    if (!emailChanged && !passwordChanged) {
      setError('Change your email or set a new password first.');
      return;
    }
    if (passwordChanged) {
      if (newPassword.length < 8) {
        setError('New password must be at least 8 characters.');
        return;
      }
      if (newPassword !== confirmPassword) {
        setError('New password and confirmation do not match.');
        return;
      }
    }

    setBusy(true);
    try {
      await updateCredentials({
        currentPassword,
        newEmail: emailChanged ? newEmail.trim() : null,
        newPassword: passwordChanged ? newPassword : null,
      });
      setNotice('Your account has been updated.');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      setError(errorMessage(err, 'Could not update your account.'));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-lg">
      <h1 className="mb-1 text-xl font-semibold text-slate-800">Account settings</h1>
      <p className="mb-5 text-sm text-slate-500">Update your login email and password.</p>

      <Card>
        <CardBody>
          <form onSubmit={handleSubmit} className="space-y-4">
            {error && <Alert kind="error">{error}</Alert>}
            {notice && <Alert kind="success">{notice}</Alert>}

            <Field label="Email">
              <Input
                type="email"
                value={newEmail}
                onChange={(e) => setNewEmail(e.target.value)}
                autoComplete="email"
              />
            </Field>

            <div className="border-t border-slate-200 pt-4">
              <p className="mb-3 text-sm font-medium text-slate-700">Change password (optional)</p>
              <div className="space-y-4">
                <Field label="New password">
                  <Input
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="Leave blank to keep current password"
                    autoComplete="new-password"
                  />
                </Field>
                <Field label="Confirm new password">
                  <Input
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    autoComplete="new-password"
                  />
                </Field>
              </div>
            </div>

            <div className="border-t border-slate-200 pt-4">
              <Field label="Current password (required to save changes)">
                <Input
                  type="password"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  autoComplete="current-password"
                />
              </Field>
            </div>

            <Button type="submit" disabled={busy} className="w-full">
              {busy ? 'Saving...' : 'Save changes'}
            </Button>
          </form>
        </CardBody>
      </Card>
    </div>
  );
}
