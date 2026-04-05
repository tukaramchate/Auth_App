import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App.tsx";
import { BrowserRouter, Routes, Route } from "react-router";
import Login from "./pages/Login.tsx";
import About from "./pages/About.tsx";
import Services from "./pages/Services.tsx";
import Signup from "./pages/Signup.tsx";
import RootLayout from "./pages/RootLayout.tsx";
import Userlayout from "./pages/users/Userlayout.tsx";
import Userhome from "./pages/users/Userhome.tsx";
import Userprofile from "./pages/users/Userprofile.tsx";
import OAuthSuccess from "./pages/OAuthSuccess.tsx";
import OAuthFailure from "./pages/OAuthFailure.tsx";
import VerifyEmail from "./pages/VerifyEmail.tsx";
import ForgotPassword from "./pages/ForgotPassword.tsx";
import ResetPassword from "./pages/ResetPassword.tsx";
import { ThemeProvider } from "./theme/ThemeProvider.tsx";
import AdminRoute from "./components/auth/AdminRoute.tsx";
import AdminLayout from "./pages/admin/AdminLayout.tsx";
import AdminDashboard from "./pages/admin/AdminDashboard.tsx";

createRoot(document.getElementById("root")!).render(
  <ThemeProvider>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<RootLayout />}>
          <Route index element={<App />} />
          <Route path="/login" element={<Login />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/verify-email" element={<VerifyEmail />} />
          <Route path="/services" element={<Services />} />
          <Route path="/about" element={<About />} />
          <Route path="/dashboard" element={<Userlayout />}>
            <Route index element={<Userhome />} />
            <Route path="profile" element={<Userprofile />} />
            {/* .... */}
          </Route>
          <Route element={<AdminRoute />}>
            <Route path="/admin" element={<AdminLayout />}>
              <Route index element={<AdminDashboard />} />
            </Route>
          </Route>
          <Route path="oauth/success" element={<OAuthSuccess />} />
          <Route path="oauth/failure" element={<OAuthFailure />} />
        </Route>
      </Routes>
    </BrowserRouter>
  </ThemeProvider>
);