import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { resetPassword } from "@/services/AuthService";
import { motion } from "framer-motion";
import { Lock } from "lucide-react";
import { useMemo, useState } from "react";
import toast from "react-hot-toast";
import { Link, useNavigate, useSearchParams } from "react-router";
import type { AxiosError } from "axios";

type ApiErrorPayload = {
  message?: string;
};

function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") || "";
  const hasToken = useMemo(() => token.trim().length > 0, [token]);

  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();

    if (!hasToken) {
      toast.error("Invalid reset link");
      return;
    }

    if (!password.trim()) {
      toast.error("Password is required");
      return;
    }

    if (password.length < 6) {
      toast.error("Password must be at least 6 characters");
      return;
    }

    if (password !== confirmPassword) {
      toast.error("Passwords do not match");
      return;
    }

    setLoading(true);
    try {
      await resetPassword(token, password);
      toast.success("Password reset successful");
      navigate("/login");
    } catch (error: unknown) {
      const axiosError = error as AxiosError<ApiErrorPayload>;
      const message =
        axiosError.response?.data?.message ||
        axiosError.message ||
        "Unable to reset password.";
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center text-foreground px-4 py-8 sm:py-10">
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45 }}
        className="w-full max-w-md aurora-panel rounded-3xl p-2"
      >
        <Card className="w-full bg-card/80 backdrop-blur-xl border-border shadow-2xl rounded-2xl p-4 sm:p-6">
          <CardContent className="space-y-5">
            <h1 className="text-2xl sm:text-3xl font-bold text-center">
              Reset Password
            </h1>

            {!hasToken ? (
              <p className="text-center text-muted-foreground">
                Reset link is invalid. Please request a new link.
              </p>
            ) : (
              <p className="text-center text-muted-foreground">
                Enter your new password below.
              </p>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="new-password">New Password</Label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                  <Input
                    id="new-password"
                    type="password"
                    className="pl-10"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                    placeholder="Enter new password"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="confirm-password">Confirm Password</Label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                  <Input
                    id="confirm-password"
                    type="password"
                    className="pl-10"
                    value={confirmPassword}
                    onChange={(event) => setConfirmPassword(event.target.value)}
                    placeholder="Confirm new password"
                  />
                </div>
              </div>

              <Button
                type="submit"
                disabled={loading || !hasToken}
                className="w-full rounded-2xl text-base sm:text-lg"
              >
                {loading ? "Resetting..." : "Reset Password"}
              </Button>

              <Link to="/forgot-password" className="block text-center text-sm text-primary">
                Request new reset link
              </Link>
            </form>
          </CardContent>
        </Card>
      </motion.div>
    </div>
  );
}

export default ResetPassword;
