import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { getEmployeeProfile } from "../services/employeeService";
import {
  clearStoredAuth,
  getStoredAuth,
  saveStoredAuth,
  saveStoredUser,
} from "./authStorage";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(getStoredAuth);
  const [loading, setLoading] = useState(true);

  const logout = useCallback(() => {
    clearStoredAuth();

    setAuth({
      token: null,
      user: null,
    });
  }, []);

  useEffect(() => {
    async function loadCurrentUser() {
      if (!auth.token) {
        setLoading(false);
        return;
      }

      if (auth.token === "mock-jwt-token") {
        setLoading(false);
        return;
      }

      if (auth.user?.role === "HR") {
        setLoading(false);
        return;
      }

      try {
        const response = await getEmployeeProfile();

        // /profile/me returns only profile fields — no role / onboardingStatus.
        // Merge so the role we set during login() isn't wiped out (otherwise
        // EmployeeRoute can't see isEmployee and bounces us back to /login).
        setAuth((current) => {
          const merged = { ...(current.user ?? {}), ...response.data };
          saveStoredUser(merged);
          return { token: current.token, user: merged };
        });
      } catch {
        logout();
      } finally {
        setLoading(false);
      }
    }

    loadCurrentUser();
  }, [auth.token, logout]);

  const login = useCallback((token, userData) => {
    saveStoredAuth(token, userData);

    setAuth({
      token,
      user: userData,
    });
  }, []);

  const updateUser = useCallback((userData) => {
    saveStoredUser(userData);

    setAuth((current) => ({
      ...current,
      user: userData,
    }));
  }, []);

  const value = useMemo(() => {
    const user = auth.user;
    const onboardingStatus = user?.onboardingStatus;

    return {
      token: auth.token,
      user,
      loading,

      login,
      logout,
      updateUser,

      isAuthenticated: Boolean(auth.token && user),

      isHR: user?.role === "HR",
      isEmployee: user?.role === "EMPLOYEE",

      onboardingStatus,

      isApproved: onboardingStatus === "APPROVED",
      isNotStarted: onboardingStatus === "NOT_STARTED",
      isPending:
        onboardingStatus === "PENDING" ||
        onboardingStatus === "WAITING_HR" ||
        onboardingStatus === "WAITING_REVIEW",
      isRejected: onboardingStatus === "REJECTED",
    };
  }, [auth, loading, login, logout, updateUser]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }

  return context;
}
