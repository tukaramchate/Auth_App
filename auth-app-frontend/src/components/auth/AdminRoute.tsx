import useAuth from "@/auth/store";
import { Navigate, Outlet } from "react-router";

function AdminRoute() {
  const checkLogin = useAuth((state) => state.checkLogin);
  const isAdmin = useAuth((state) => state.isAdmin);

  if (!checkLogin()) {
    return <Navigate to="/login" replace />;
  }

  if (!isAdmin()) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}

export default AdminRoute;
