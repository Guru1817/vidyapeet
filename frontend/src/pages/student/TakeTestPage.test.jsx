// Unit tests for the student TakeTestPage: section grouping vs. ungrouped
// fallback, the single overall timer display, and auto-submit at zero.
//
// Validates: Requirements 7.6, 7.7, 7.8
//   7.5 - a sectioned test renders questions grouped under their section labels
//   7.6 - while taking a (sectioned) test the single Overall_Timer is displayed
//   7.7 - when the Overall_Timer reaches zero the attempt auto-submits
//   7.8 - a test with no sections renders as an ungrouped list of questions
//
// TakeTestPage starts an attempt via POST /api/student/tests/{id}/start, derives
// a single countdown from attempt.deadline, and on zero POSTs to
// /api/student/attempts/{id}/submit then navigates to the result view. We mock
// the api client, react-router (useParams/useNavigate), and useBranding, and
// drive the countdown with fake timers.
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, act } from '@testing-library/react';

// API client: start/submit are POSTs; image GETs return a blob (unused here
// because test questions carry no imageKey).
const { post, get } = vi.hoisted(() => ({
  post: vi.fn(),
  get: vi.fn(() => Promise.resolve({ data: new Blob() })),
}));
vi.mock('../../api/client', () => ({
  default: { post, get },
  errorMessage: (_err, fallback = 'error') => fallback,
}));

// Routing: capture navigation and pin the :testId route param.
const navigate = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, useNavigate: () => navigate, useParams: () => ({ testId: 't1' }) };
});

// Branding: TakeTestPage only reads branding.name in the header.
vi.mock('../../branding/BrandingContext', () => ({
  useBranding: () => ({ name: 'Test Institute' }),
}));

import TakeTestPage from './TakeTestPage.jsx';

// A minimal MCQ question the take-test view can render.
function question(id, text, sectionId = null) {
  return {
    id,
    text,
    type: 'MCQ',
    marks: 1,
    sectionId,
    optionA: 'a',
    optionB: 'b',
    optionC: 'c',
    optionD: 'd',
  };
}

// Build the /start response. `secondsToDeadline` is relative to the (fake) now.
function startResponse({ sections = [], questions, secondsToDeadline = 600 }) {
  return {
    data: {
      attemptId: 'a1',
      title: 'Sample Test',
      deadline: new Date(Date.now() + secondsToDeadline * 1000).toISOString(),
      sections,
      questions,
    },
  };
}

// Route the mocked POSTs by endpoint.
function mockStart(attempt) {
  post.mockImplementation((url) => {
    if (url.endsWith('/start')) return Promise.resolve(attempt);
    if (url.endsWith('/submit')) return Promise.resolve({ data: {} });
    return Promise.resolve({ data: {} });
  });
}

// Flush pending promise microtasks (start/submit resolutions) inside act so
// React state updates are applied before assertions.
async function flush() {
  await act(async () => {
    await Promise.resolve();
    await Promise.resolve();
    await Promise.resolve();
  });
}

beforeEach(() => {
  vi.useFakeTimers();
  post.mockReset();
  get.mockClear();
  navigate.mockReset();
});

afterEach(() => {
  vi.useRealTimers();
});

describe('ungrouped fallback (Req 7.8)', () => {
  it('renders questions as a flat list with no section headings when there are no sections', async () => {
    mockStart(
      startResponse({
        sections: [],
        questions: [question('q1', 'What is 2 + 2?'), question('q2', 'Capital of France?')],
      }),
    );

    render(<TakeTestPage />);
    await flush();

    // Both questions render, numbered sequentially.
    expect(screen.getByText(/What is 2 \+ 2\?/)).toBeInTheDocument();
    expect(screen.getByText(/Capital of France\?/)).toBeInTheDocument();

    // No section grouping headings are present (section labels render as <h2>).
    expect(screen.queryAllByRole('heading', { level: 2 })).toHaveLength(0);
  });
});

describe('section grouping (Req 7.5)', () => {
  it('renders questions grouped under their section labels', async () => {
    mockStart(
      startResponse({
        sections: [
          { id: 's1', label: 'Quantitative', position: 0 },
          { id: 's2', label: 'Verbal', position: 1 },
        ],
        questions: [
          question('q1', 'What is 2 + 2?', 's1'),
          question('q2', 'Synonym of happy?', 's2'),
        ],
      }),
    );

    render(<TakeTestPage />);
    await flush();

    // Each section label appears as a heading.
    expect(screen.getByRole('heading', { level: 2, name: /Quantitative/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: /Verbal/i })).toBeInTheDocument();

    // Questions still render within their groups.
    expect(screen.getByText(/What is 2 \+ 2\?/)).toBeInTheDocument();
    expect(screen.getByText(/Synonym of happy\?/)).toBeInTheDocument();
  });
});

describe('overall timer (Req 7.6)', () => {
  it('displays a single overall countdown derived from the deadline', async () => {
    // 65 seconds remaining => "01:05".
    mockStart(startResponse({ sections: [], questions: [question('q1', 'Q?')], secondsToDeadline: 65 }));

    render(<TakeTestPage />);
    await flush();

    const timer = screen.getByTitle('Time remaining');
    expect(timer).toBeInTheDocument();
    expect(timer).toHaveTextContent('01:05');
  });
});

describe('auto-submit at zero (Req 7.7)', () => {
  it('auto-submits the attempt and navigates to the result view when the timer hits zero', async () => {
    // 3 seconds on the clock; drive it down to zero.
    mockStart(startResponse({ sections: [], questions: [question('q1', 'Q?')], secondsToDeadline: 3 }));

    render(<TakeTestPage />);
    await flush();

    // Timer starts at 3 seconds and no submit has happened yet.
    expect(screen.getByTitle('Time remaining')).toHaveTextContent('00:03');
    expect(post).not.toHaveBeenCalledWith(
      '/api/student/attempts/a1/submit',
      expect.anything(),
    );

    // Advance the countdown one second at a time to zero.
    for (let i = 0; i < 3; i += 1) {
      await act(async () => {
        vi.advanceTimersByTime(1000);
      });
    }
    await flush();

    // The attempt auto-submits to the submit endpoint...
    expect(post).toHaveBeenCalledWith(
      '/api/student/attempts/a1/submit',
      expect.objectContaining({
        answers: expect.arrayContaining([{ questionId: 'q1', answer: null }]),
      }),
    );
    // ...and the student is routed to the result view.
    expect(navigate).toHaveBeenCalledWith('/student/tests/t1/result', { replace: true });
  });
});
