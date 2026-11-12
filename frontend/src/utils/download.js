export function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => window.URL.revokeObjectURL(url), 1000);
}

export function filenameFromResponse(response, fallback) {
  const header = response?.headers?.['content-disposition'];
  if (header) {
    const star = /filename\*=(?:UTF-8'')?([^;]+)/i.exec(header);
    if (star) {
      try {
        return decodeURIComponent(star[1].trim().replace(/"/g, ''));
      } catch {
        /* fall through */
      }
    }
    const plain = /filename="?([^";]+)"?/i.exec(header);
    if (plain) return plain[1].trim();
  }
  return fallback;
}

/** Saves an axios blob response using Content-Disposition filename when available. */
export function saveResponse(response, fallbackName) {
  downloadBlob(response.data, filenameFromResponse(response, fallbackName));
}
