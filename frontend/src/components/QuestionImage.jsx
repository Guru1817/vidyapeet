import { useEffect, useState } from 'react';
import api from '../api/client';

/**
 * Renders a question's attached image. The image endpoint
 * (`GET /api/questions/{id}/image`) requires the JWT `Authorization` header, so
 * a plain `<img src>` cannot be used — we fetch the bytes as a blob (which sends
 * the token via the axios interceptor) and render an object URL instead.
 *
 * Renders nothing unless the question carries an `imageKey` (camelCase from the
 * API; `image_key` is accepted as a fallback).
 */
export default function QuestionImage({ questionId, imageKey, alt = 'Question image', className = '' }) {
  const hasImage = Boolean(imageKey);
  const [src, setSrc] = useState(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    if (!hasImage || !questionId) return undefined;
    let cancelled = false;
    let objectUrl = null;
    setFailed(false);
    setSrc(null);
    api
      .get(`/api/questions/${questionId}/image`, { responseType: 'blob' })
      .then((res) => {
        if (cancelled) return;
        objectUrl = window.URL.createObjectURL(res.data);
        setSrc(objectUrl);
      })
      .catch(() => {
        if (!cancelled) setFailed(true);
      });
    return () => {
      cancelled = true;
      if (objectUrl) window.URL.revokeObjectURL(objectUrl);
    };
  }, [questionId, imageKey, hasImage]);

  if (!hasImage || failed) return null;
  if (!src) {
    return (
      <div
        className={`mt-2 h-32 w-full max-w-sm animate-pulse rounded-lg bg-slate-100 dark:bg-slate-700 ${className}`}
        aria-hidden="true"
      />
    );
  }
  return (
    <img
      src={src}
      alt={alt}
      className={`mt-2 max-h-72 w-auto max-w-full rounded-lg border border-slate-200 dark:border-slate-700 ${className}`}
    />
  );
}
