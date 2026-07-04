import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import ProtectedRoute, { homeFor } from './auth/ProtectedRoute';
import PortalLayout from './components/PortalLayout';
import LoginPage from './pages/LoginPage';
import InstitutesPage from './pages/superadmin/InstitutesPage';
import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import StudentsPage from './pages/admin/StudentsPage';
import BatchesPage from './pages/admin/BatchesPage';
import BatchDetailPage from './pages/admin/BatchDetailPage';
import TestEditorPage from './pages/admin/TestEditorPage';
import LibraryPage from './pages/admin/LibraryPage';
import LibraryFolderPage from './pages/admin/LibraryFolderPage';
import AdminPerformancePage from './pages/admin/AdminPerformancePage';
import AdminStudentPerformancePage from './pages/admin/AdminStudentPerformancePage';
import StudentTestsPage from './pages/student/StudentTestsPage';
import StudentNotesPage from './pages/student/StudentNotesPage';
import StudentPerformancePage from './pages/student/StudentPerformancePage';
import TakeTestPage from './pages/student/TakeTestPage';
import ResultPage from './pages/student/ResultPage';
import LeaderboardPage from './pages/student/LeaderboardPage';

function RootRedirect() {
  const { user } = useAuth();
  return <Navigate to={user ? homeFor(user.role) : '/login'} replace />;
}

const ADMIN_NAV = [
  { to: '/admin/dashboard', label: 'Dashboard' },
  { to: '/admin/batches', label: 'Batches' },
  { to: '/admin/students', label: 'Students' },
  { to: '/admin/library', label: 'Library' },
  { to: '/admin/performance', label: 'Performance' },
];
const STUDENT_NAV = [
  { to: '/student/tests', label: 'Tests' },
  { to: '/student/notes', label: 'Notes' },
  { to: '/student/performance', label: 'Performance' },
];
const SUPERADMIN_NAV = [{ to: '/superadmin/institutes', label: 'Institutes' }];

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<RootRedirect />} />
      <Route path="/login" element={<LoginPage />} />

      {/* Platform owner */}
      <Route
        element={
          <ProtectedRoute roles={['SUPER_ADMIN']}>
            <PortalLayout navItems={SUPERADMIN_NAV} />
          </ProtectedRoute>
        }
      >
        <Route path="/superadmin/institutes" element={<InstitutesPage />} />
      </Route>

      {/* Institute admin */}
      <Route
        element={
          <ProtectedRoute roles={['INSTITUTE_ADMIN']}>
            <PortalLayout navItems={ADMIN_NAV} />
          </ProtectedRoute>
        }
      >
        <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
        <Route path="/admin/students" element={<StudentsPage />} />
        <Route path="/admin/batches" element={<BatchesPage />} />
        <Route path="/admin/batches/:batchId" element={<BatchDetailPage />} />
        <Route path="/admin/tests/:testId" element={<TestEditorPage />} />
        <Route path="/admin/tests/:testId/leaderboard" element={<LeaderboardPage />} />
        <Route path="/admin/library" element={<LibraryPage />} />
        <Route path="/admin/library/folders/:folderId" element={<LibraryFolderPage />} />
        <Route path="/admin/performance" element={<AdminPerformancePage />} />
        <Route path="/admin/performance/:studentId" element={<AdminStudentPerformancePage />} />
      </Route>

      {/* Student */}
      <Route
        element={
          <ProtectedRoute roles={['STUDENT']}>
            <PortalLayout navItems={STUDENT_NAV} />
          </ProtectedRoute>
        }
      >
        <Route path="/student/tests" element={<StudentTestsPage />} />
        <Route path="/student/notes" element={<StudentNotesPage />} />
        <Route path="/student/performance" element={<StudentPerformancePage />} />
        <Route path="/student/tests/:testId/result" element={<ResultPage />} />
        <Route path="/student/tests/:testId/leaderboard" element={<LeaderboardPage />} />
      </Route>

      {/* Test-taking runs full-screen (its own layout) */}
      <Route
        path="/student/tests/:testId/take"
        element={
          <ProtectedRoute roles={['STUDENT']}>
            <TakeTestPage />
          </ProtectedRoute>
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
