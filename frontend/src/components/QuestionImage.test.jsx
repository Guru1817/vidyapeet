// Unit tests for QuestionImage rendering.
//
// Feature: vidyapeeth-v2-upgrades, Task 6.7 - Write unit tests for image rendering
// Validates:
//   5.5 - editor view renders the image when the question has an image
//   5.6 - take-test view renders the image when the question has an image
//   5.7 - result view renders the image when the question has an image
//
// The editor, take-test, and result views all render the shared QuestionImage
// component, passing `imageKey={x.imageKey || x.image_key}`. Testing the
// component directly covers the rendering behavior for all three views: it
// renders an <img> when an image key is present (from either the camelCase
// `imageKey` or the snake_case `image_key` API field) and renders nothing when
// no key is present.
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';

// Mock the api client so no real HTTP request is made; `get` resolves with a
// blob just like the real `GET /api/questions/{id}/image` endpoint.
const get = vi.fn();
vi.mock('../api/client', () => ({
  default: { get: (...args) => get(...args) },
}));

import QuestionImage from './QuestionImage.jsx';

const OBJECT_URL = 'blob:mock-object-url';

beforeEach(() => {
  get.mockReset();
  get.mockResolvedValue({ data: new Blob(['img-bytes'], { type: 'image/png' }) });
  // jsdom does not implement object URL APIs; provide them.
  window.URL.createObjectURL = vi.fn(() => OBJECT_URL);
  window.URL.revokeObjectURL = vi.fn();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('QuestionImage', () => {
  it('renders the image when a camelCase imageKey is present', async () => {
    render(<QuestionImage questionId={42} imageKey="questions/42.png" />);

    const img = await screen.findByRole('img');
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', OBJECT_URL);
    expect(get).toHaveBeenCalledWith('/api/questions/42/image', { responseType: 'blob' });
  });

  it('renders the image when the snake_case image_key value is passed through', async () => {
    // The three views pass `imageKey={x.imageKey || x.image_key}`, so an
    // API payload using only `image_key` still yields a rendered image.
    const item = { image_key: 'questions/7.png' };
    render(<QuestionImage questionId={7} imageKey={item.imageKey || item.image_key} />);

    const img = await screen.findByRole('img');
    expect(img).toBeInTheDocument();
    expect(get).toHaveBeenCalledWith('/api/questions/7/image', { responseType: 'blob' });
  });

  it('renders nothing and makes no request when no image key is present', () => {
    const { container } = render(<QuestionImage questionId={99} imageKey={null} />);

    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(container).toBeEmptyDOMElement();
    expect(get).not.toHaveBeenCalled();
  });

  it('renders nothing when the field is absent (undefined imageKey)', () => {
    const item = {}; // neither imageKey nor image_key
    const { container } = render(
      <QuestionImage questionId={5} imageKey={item.imageKey || item.image_key} />,
    );

    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(container).toBeEmptyDOMElement();
    expect(get).not.toHaveBeenCalled();
  });

  it('renders nothing when the image fetch fails', async () => {
    get.mockRejectedValueOnce(new Error('network error'));
    const { container } = render(<QuestionImage questionId={3} imageKey="questions/3.png" />);

    await waitFor(() => expect(get).toHaveBeenCalled());
    await waitFor(() => expect(screen.queryByRole('img')).not.toBeInTheDocument());
    // After a failed load the component collapses to nothing.
    await waitFor(() => expect(container).toBeEmptyDOMElement());
  });

  it('uses the provided alt text on the rendered image', async () => {
    render(<QuestionImage questionId={11} imageKey="questions/11.png" alt="Diagram" />);

    const img = await screen.findByRole('img');
    expect(img).toHaveAttribute('alt', 'Diagram');
  });
});
