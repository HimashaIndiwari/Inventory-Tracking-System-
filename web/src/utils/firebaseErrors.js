// Firebase Auth error codes → user-facing messages. Unknown errors fall back to a generic message.
const MESSAGES = {
  'auth/invalid-credential': 'Incorrect email or password.',
  'auth/invalid-email': 'Enter a valid email address.',
  'auth/user-disabled': 'This account has been disabled.',
  'auth/too-many-requests': 'Too many attempts. Please wait a moment and try again.',
  'auth/email-already-in-use': 'An account with this email already exists.',
  'auth/weak-password': 'Password is too weak. Use at least 8 characters.',
  'auth/network-request-failed': 'Network error. Check your connection and try again.',
};

export function firebaseErrorMessage(error) {
  return MESSAGES[error?.code] || error?.message || 'Something went wrong. Please try again.';
}
