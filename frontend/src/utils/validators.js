export const MOBILE_REGEX = /^[0-9]{10}$/;
export const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const GST_REGEX = /^[0-9A-Za-z]{15}$/;
export const PAN_REGEX = /^[0-9A-Za-z]{10}$/;
export const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;

export const PASSWORD_HINT = 'Min 8 characters with at least one uppercase, one lowercase and one digit';

const isBlank = (v) => v === undefined || v === null || String(v).trim() === '';

export const rules = {
  required: (label = 'This field') => ({
    validate: (v) => !isBlank(v) || `${label} is required`,
  }),
  mobile: (required = true) => ({
    validate: (v) => {
      if (isBlank(v)) return required ? 'Mobile number is required' : true;
      return MOBILE_REGEX.test(String(v).trim()) || 'Mobile number must be exactly 10 digits';
    },
  }),
  email: (required = false) => ({
    validate: (v) => {
      if (isBlank(v)) return required ? 'Email is required' : true;
      return EMAIL_REGEX.test(String(v).trim()) || 'Enter a valid email address';
    },
  }),
  gst: () => ({
    validate: (v) => isBlank(v) || GST_REGEX.test(String(v).trim()) || 'GSTIN must be 15 characters',
  }),
  pan: () => ({
    validate: (v) => isBlank(v) || PAN_REGEX.test(String(v).trim()) || 'PAN must be 10 characters',
  }),
  positive: (label = 'Value', required = true) => ({
    validate: (v) => {
      if (isBlank(v)) return required ? `${label} is required` : true;
      const n = Number(v);
      if (!Number.isFinite(n)) return `${label} must be a number`;
      return n > 0 || `${label} must be greater than 0`;
    },
  }),
  nonNegative: (label = 'Value', required = true) => ({
    validate: (v) => {
      if (isBlank(v)) return required ? `${label} is required` : true;
      const n = Number(v);
      if (!Number.isFinite(n)) return `${label} must be a number`;
      return n >= 0 || `${label} cannot be negative`;
    },
  }),
  password: (required = true) => ({
    validate: (v) => {
      if (isBlank(v)) return required ? 'Password is required' : true;
      return PASSWORD_REGEX.test(String(v)) || PASSWORD_HINT;
    },
  }),
  maxLength: (n, label = 'Value') => ({
    validate: (v) => isBlank(v) || String(v).length <= n || `${label} must be at most ${n} characters`,
  }),
};

/** Merge several rule objects ({validate}) into one */
export function combine(...ruleSets) {
  const fns = ruleSets.filter(Boolean).map((r) => r.validate);
  return {
    validate: (v, form) => {
      for (const fn of fns) {
        const res = fn(v, form);
        if (res !== true) return res;
      }
      return true;
    },
  };
}
