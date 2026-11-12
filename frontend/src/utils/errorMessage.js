/** Extract a readable message from an axios error following the backend error format. */
export function getErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  const data = error?.response?.data;
  if (data && typeof data === 'object' && !(data instanceof Blob)) {
    if (data.message) return data.message;
    if (data.error) return data.error;
  }
  if (error?.response) return fallback;
  if (error?.code === 'ERR_NETWORK') return 'Unable to reach the server. Check your connection.';
  return error?.message || fallback;
}

export function getFieldErrors(error) {
  const fe = error?.response?.data?.fieldErrors;
  return fe && typeof fe === 'object' ? fe : null;
}

const toFormPath = (name) => name.replace(/\[(\d+)\]/g, '.$1');

/**
 * Maps backend `fieldErrors` into react-hook-form errors.
 * Returns true when at least one field error was applied.
 */
export function applyFieldErrors(error, setError) {
  const fe = getFieldErrors(error);
  if (!fe || !setError) return false;
  const entries = Object.entries(fe);
  entries.forEach(([field, message]) => {
    setError(toFormPath(field), { type: 'server', message: String(message) });
  });
  return entries.length > 0;
}

/** Shows the backend message (with field errors when present) and maps field errors to the form. */
export function handleFormError(error, { setError, notify, fallback } = {}) {
  applyFieldErrors(error, setError);
  const fe = getFieldErrors(error);
  let message = getErrorMessage(error, fallback);
  if (fe) {
    const details = Object.entries(fe)
      .map(([k, v]) => `${k}: ${v}`)
      .join('; ');
    if (details) message = `${message} - ${details}`;
  }
  notify?.error(message);
}
