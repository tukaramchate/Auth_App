import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { forgotPassword } from "@/services/AuthService";
import { motion } from "framer-motion";
import { Mail } from "lucide-react";
import { useState } from "react";
import toast from "react-hot-toast";
import { Link } from "react-router";
import type { AxiosError } from "axios";

type ApiErrorPayload = {
  message?: string;
};

function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();

    if (!email.trim()) {
      toast.error("Email is required");
      return;
    }

    setLoading(true);
    try {
      await forgotPassword(email.trim());
      toast.success("If the email exists, a reset link has been sent.");
    } catch (error: unknown) {
      const axiosError = error as AxiosError<ApiErrorPayload>;
      const message =
        axiosError.response?.data?.message ||
        axiosError.message ||
        "Unable to process request.";
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
              Forgot Password
            </h1>
            <p className="text-center text-muted-foreground">
              Enter your email and we will send a reset link.
            </p>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="forgot-email">Email</Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                  <Input
                    id="forgot-email"
                    type="email"
                    className="pl-10"
                    value={email}
                    onChange={(event) => setEmail(event.target.value)}
                    placeholder="you@example.com"
                  />
                </div>
              </div>

              <Button
                type="submit"
                disabled={loading}
                className="w-full rounded-2xl text-base sm:text-lg"
              >
                {loading ? "Sending..." : "Send reset link"}
              </Button>

              <Link to="/login" className="block text-center text-sm text-primary">
                Back to Login
              </Link>
            </form>
          </CardContent>
        </Card>
      </motion.div>
    </div>
  );
}

export default ForgotPassword;
