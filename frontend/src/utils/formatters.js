const currencyFmt = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});
const numberFmt = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 3 });
const compactFmt = new Intl.NumberFormat('en-IN', { notation: 'compact', maximumFractionDigits: 1 });

const isEmpty = (v) => v === null || v === undefined || v === '' || Number.isNaN(Number(v));

export function formatCurrency(value) {
  if (isEmpty(value)) return '-';
  return currencyFmt.format(Number(value));
}

export function formatNumber(value) {
  if (isEmpty(value)) return '-';
  return numberFmt.format(Number(value));
}

export function formatCompact(value) {
  if (isEmpty(value)) return '';
  return compactFmt.format(Number(value));
}

/** yyyy-MM-dd (or ISO) -> dd/MM/yyyy */
export function formatDate(value) {
  if (!value) return '-';
  const s = String(value);
  const m = /^(\d{4})-(\d{2})-(\d{2})/.exec(s);
  if (m) return `${m[3]}/${m[2]}/${m[1]}`;
  return s;
}

export function formatDateTime(value) {
  if (!value) return '-';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return String(value);
  return d.toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' });
}

const pad = (n) => String(n).padStart(2, '0');

/** Local-time Date -> yyyy-MM-dd */
export function toISODate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export const today = () => toISODate(new Date());

export function humanize(value) {
  if (!value) return '-';
  return String(value)
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}

export function toNumber(value, fallback = 0) {
  const n = Number(value);
  return Number.isFinite(n) ? n : fallback;
}

export const round2 = (n) => Math.round((toNumber(n) + Number.EPSILON) * 100) / 100;

/** Format a report cell according to its column type */
export function formatByType(value, type) {
  if (value === null || value === undefined || value === '') return '-';
  switch (type) {
    case 'CURRENCY':
      return formatCurrency(value);
    case 'NUMBER':
      return formatNumber(value);
    case 'DATE':
      return formatDate(value);
    default:
      return String(value);
  }
}
