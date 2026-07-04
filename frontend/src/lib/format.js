// Formats a possibly-fractional score without trailing ".0" noise.
export function formatScore(n) {
  if (n === null || n === undefined) return '-';
  const num = Number(n);
  if (Number.isInteger(num)) return String(num);
  return num.toFixed(2).replace(/\.?0+$/, '');
}
