import type LoginData from "@/models/LoginData";
import type LoginResponseData from "@/models/LoginResponseData";
import type User from "@/models/User";
import { loginUser, logoutUser } from "@/services/AuthService";
import { create } from "zustand";
import { persist, type PersistOptions } from "zustand/middleware";

const LOCAL_KEY = "app_state";
//type AuthStatus = "idle" | "authenticating" | "authenticated" | "anonymous";

//global authstate:

type AuthState = {
  accessToken: string | null;
  user: User | null;
  authStatus: boolean;
  authLoading: boolean;
  hasRole: (roleName: string) => boolean;
  isAdmin: () => boolean;
  login: (loginData: LoginData) => Promise<LoginResponseData>;
  logout: (silent?: boolean) => void;
  checkLogin: () => boolean | undefined;
  updateCurrentUser: (user: User) => void;

  changeLocalLoginData: (
    accessToken: string,
    user: User,
    authStatus: boolean
  ) => void;
};

type AuthPersistState = Pick<AuthState, "user" | "authStatus">;

const persistOptions: PersistOptions<AuthState, AuthPersistState> = {
  name: LOCAL_KEY,
  partialize: (state) => ({
    user: state.user,
    authStatus: state.authStatus,
  }),
};

//main logic for global state
const useAuth = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      user: null,
      authStatus: false,
      authLoading: false,

      hasRole: (roleName) => {
        const roles = get().user?.roles ?? [];
        return roles.some((role) => role.name === roleName);
      },

      isAdmin: () => {
        const roles = get().user?.roles ?? [];
        return roles.some((role) => role.name === "ADMIN");
      },

      changeLocalLoginData: (accessToken, user, authStatus) => {
        set({
          accessToken,
          user,
          authStatus,
        });
      },
      updateCurrentUser: (user) => {
        set((state) => ({
          user: {
            ...(state.user ?? {}),
            ...user,
          } as User,
        }));
      },
      login: async (loginData) => {
        set({ authLoading: true });
        try {
          const loginResponseData = await loginUser(loginData);
          set({
            accessToken: loginResponseData.accessToken,
            user: loginResponseData.user,
            authStatus: true,
          });
          return loginResponseData;
        } finally {
          set({
            authLoading: false,
          });
        }
      },
      logout: async () => {
        try {
          //   if (!silent) {
          //     await logoutUser();
          //   }
          set({
            authLoading: true,
          });
          await logoutUser();
        } catch {
          // Ignore remote logout failures and clear local session regardless.
        } finally {
          set({
            authLoading: false,
          });
        }
        // await logoutUser();
        set({
          accessToken: null,
          user: null,
          authLoading: false,
          authStatus: false,
        });
      },
      checkLogin: () => {
        if (get().accessToken && get().authStatus) return true;
        return false;
      },
    }),

    persistOptions
  )
);

export default useAuth;