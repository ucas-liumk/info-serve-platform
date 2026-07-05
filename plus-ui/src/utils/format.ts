export const formatStat = (value?: number | string | null) => {
  const numeric = Number(value || 0);
  if (!Number.isFinite(numeric) || numeric === 0) {
    return '—';
  }
  return numeric.toLocaleString('en-US');
};
