import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import {
  createUserWithEmailAndPassword,
  onAuthStateChanged,
  sendEmailVerification,
  signInWithEmailAndPassword,
  signOut,
  updateProfile,
} from 'firebase/auth';
import { auth } from '../firebase';
import { setTokenProvider, setUnauthorizedHandler } from '../services/apiClient';
import { authService } from '../services/services';

const AuthContext = createContext(null);

/**
 * Holds the Firebase user and the backend profile ({ id, email, displayName, role }).
 * The role always comes from GET /api/auth/me; the frontend never decides it.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [emailVerified, setEmailVerified] = useState(false);

  const loadProfile = useCallback(async () => {
    try {
      setError(null);
      setProfile(await authService.me());
    } catch (e) {
      setProfile(null);
      setError(e);
    }
  }, []);

  useEffect(() => {
    setTokenProvider(async () => (auth.currentUser ? auth.currentUser.getIdToken() : null));
    setUnauthorizedHandler(() => signOut(auth));
    return onAuthStateChanged(auth, async (firebaseUser) => {
      setUser(firebaseUser);
      setEmailVerified(Boolean(firebaseUser?.emailVerified));
      if (firebaseUser) {
        await loadProfile();
      } else {
        setProfile(null);
        setError(null);
      }
      setLoading(false);
    });
  }, [loadProfile]);

  const login = useCallback((email, password) => signInWithEmailAndPassword(auth, email.trim(), password), []);

  const register = useCallback(async (email, displayName, password) => {
    const { user: created } = await createUserWithEmailAndPassword(auth, email.trim(), password);
    await updateProfile(created, { displayName: displayName.trim() });
    await sendEmailVerification(created);
    await created.getIdToken(true); // token must carry the display name before /api/auth/me
    await loadProfile();
  }, [loadProfile]);

  /** FR-AUTH-11: reload + forced token refresh so the backend sees email_verified = true. */
  const refreshVerification = useCallback(async () => {
    const current = auth.currentUser;
    if (!current) return;
    await current.reload();
    await current.getIdToken(true);
    setEmailVerified(Boolean(auth.currentUser?.emailVerified));
    await loadProfile();
  }, [loadProfile]);

  const logout = useCallback(() => signOut(auth), []);

  const value = useMemo(() => ({
    user,
    profile,
    loading,
    error,
    emailVerified,
    isAdmin: profile?.role === 'ADMIN',
    login,
    register,
    logout,
    retryProfile: loadProfile,
    refreshVerification,
  }), [user, profile, loading, error, emailVerified, login, register, logout, loadProfile, refreshVerification]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}

export { AuthContext };
