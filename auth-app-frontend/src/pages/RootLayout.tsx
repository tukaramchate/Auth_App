import { Outlet } from "react-router";
import Navbar from "../components/Navbar";
import { Toaster } from "react-hot-toast";
import { useEffect, useRef } from "react";
import useAuth from "@/auth/store";
import { refreshToken } from "@/services/AuthService";

function RootLayout() {
  const hasRehydrated = useRef(false);
  const accessToken = useAuth((state) => state.accessToken);
  const changeLocalLoginData = useAuth((state) => state.changeLocalLoginData);

  useEffect(() => {
    if (hasRehydrated.current || accessToken) {
      return;
    }

    hasRehydrated.current = true;
    const hydrateSession = async () => {
      try {
        const response = await refreshToken();
        changeLocalLoginData(response.accessToken, response.user, true);
      } catch {
        // Ignore hydration failures for anonymous sessions.
      }
    };

    hydrateSession();
  }, [accessToken, changeLocalLoginData]);

  return (
    <div className="min-h-screen">
      <Toaster />
      <Navbar />
      <main className="mx-auto w-full">
        <Outlet />
      </main>
    </div>
  );
}

export default RootLayout;