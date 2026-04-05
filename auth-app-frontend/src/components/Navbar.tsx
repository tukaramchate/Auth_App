import { Button } from "./ui/button";
import { NavLink, useNavigate } from "react-router";
import useAuth from "@/auth/store";
import { useState } from "react";
import { Menu, Moon, Sun, X } from "lucide-react";
import { AnimatePresence, motion } from "framer-motion";
import { useTheme } from "@/theme/useTheme";
import logo from "@/assets/logo.jpg";

function Navbar() {
  const checkLogin = useAuth((state) => state.checkLogin);
  const user = useAuth((state) => state.user);
  const isAdmin = useAuth((state) => state.isAdmin);
  const logout = useAuth((state) => state.logout);
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const { theme, toggleTheme } = useTheme();

  const authenticated = !!checkLogin();

  const handleLogout = () => {
    logout();
    setOpen(false);
    navigate("/");
  };

  const closeMenu = () => setOpen(false);

  return (
    <header className="sticky top-0 z-50 border-b border-border/60 bg-background/80 backdrop-blur-xl">
      <nav className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
        <div className="font-semibold items-center flex gap-2">
          <img
            src={logo}
            alt="Authenfy logo"
            className="h-9 w-9 rounded-md object-cover shadow-sm ring-1 ring-border"
          />
          <span className="text-base tracking-tight">AUTHENFY</span>
        </div>

        <button
          className="md:hidden rounded-lg border border-border p-2"
          onClick={() => setOpen((value) => !value)}
          aria-label="Toggle navigation"
        >
          {open ? <X size={18} /> : <Menu size={18} />}
        </button>

        <div className="hidden md:flex gap-3 items-center">
          <Button
            size="icon"
            variant="outline"
            className="h-9 w-9"
            onClick={toggleTheme}
            aria-label="Toggle theme"
            title={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
          >
            {theme === "dark" ? <Sun size={16} /> : <Moon size={16} />}
          </Button>

          {authenticated ? (
            <>
              <NavLink
                to="/dashboard/profile"
                className="rounded-lg px-2 py-1 text-sm text-muted-foreground transition-colors hover:text-foreground"
              >
                {user?.name}
              </NavLink>

              {isAdmin() && (
                <NavLink
                  to="/admin"
                  className="rounded-lg px-2 py-1 text-sm text-muted-foreground transition-colors hover:text-foreground"
                >
                  Admin
                </NavLink>
              )}

              <Button
                onClick={handleLogout}
                size="sm"
                className="cursor-pointer"
                variant="outline"
              >
                Logout
              </Button>
            </>
          ) : (
            <>
              <NavLink
                to="/"
                className="rounded-lg px-2 py-1 text-sm text-muted-foreground transition-colors hover:text-foreground"
              >
                Home
              </NavLink>
              <NavLink to="/login">
                <Button size="sm" className="cursor-pointer" variant="outline">
                  Login
                </Button>
              </NavLink>
              <NavLink to="/signup">
                <Button size="sm" className="cursor-pointer" variant="outline">
                  Signup
                </Button>
              </NavLink>
            </>
          )}
        </div>
      </nav>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="md:hidden overflow-hidden border-t border-border/60 bg-background/95"
          >
            <div className="mx-auto flex max-w-6xl flex-col gap-2 px-4 py-3 sm:px-6">
              <Button
                variant="outline"
                onClick={toggleTheme}
                className="w-full"
                aria-label="Toggle theme"
              >
                {theme === "dark" ? (
                  <>
                    <Sun size={16} />
                    <span>Light Mode</span>
                  </>
                ) : (
                  <>
                    <Moon size={16} />
                    <span>Dark Mode</span>
                  </>
                )}
              </Button>

              {authenticated ? (
                <>
                  <NavLink
                    to="/dashboard/profile"
                    onClick={closeMenu}
                    className="rounded-lg px-3 py-2 text-sm text-muted-foreground hover:bg-muted"
                  >
                    {user?.name}
                  </NavLink>

                  {isAdmin() && (
                    <NavLink
                      to="/admin"
                      onClick={closeMenu}
                      className="rounded-lg px-3 py-2 text-sm text-muted-foreground hover:bg-muted"
                    >
                      Admin
                    </NavLink>
                  )}

                  <Button onClick={handleLogout} className="w-full" variant="outline">
                    Logout
                  </Button>
                </>
              ) : (
                <>
                  <NavLink
                    to="/"
                    onClick={closeMenu}
                    className="rounded-lg px-3 py-2 text-sm text-muted-foreground hover:bg-muted"
                  >
                    Home
                  </NavLink>

                  <NavLink to="/login" onClick={closeMenu}>
                    <Button className="w-full" variant="outline">
                      Login
                    </Button>
                  </NavLink>

                  <NavLink to="/signup" onClick={closeMenu}>
                    <Button className="w-full" variant="outline">
                      Signup
                    </Button>
                  </NavLink>
                </>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </header>
  );
}

export default Navbar;