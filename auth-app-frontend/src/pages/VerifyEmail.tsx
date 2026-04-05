import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { resendVerification, verifyEmail } from "@/services/AuthService";
import { motion } from "framer-motion";
import { useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";
import { Link, useSearchParams } from "react-router";
import type { AxiosError } from "axios";

type ApiErrorPayload = {
  message?: string;
};

function VerifyEmail() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const initialEmail = searchParams.get("email") || "";

  const [email, setEmail] = useState(initialEmail);
  const [verifying, setVerifying] = useState(false);
  const [resending, setResending] = useState(false);
  const [verified, setVerified] = useState(false);

  const hasToken = useMemo(() => !!token && token.trim().length > 0, [token]);

  useEffect(() => {
    async function runVerification() {
      if (!hasToken || !token) {
        return;
      }

      setVerifying(true);
      try {
        await verifyEmail(token);
        setVerified(true);
        toast.success("Email verified successfully.");
      } catch (error: unknown) {
        const axiosError = error as AxiosError<ApiErrorPayload>;
        const message =
          axiosError.response?.data?.message ||
          axiosError.message ||
          "Unable to verify email.";
        toast.error(message);
      } finally {
        setVerifying(false);
      }
    }

    runVerification();
  }, [hasToken, token]);

  const handleResend = async (event: React.FormEvent) => {
    event.preventDefault();

    if (!email.trim()) {
      toast.error("Email is required");
      return;
    }

    setResending(true);
    try {
      await resendVerification(email.trim());
      toast.success("Verification email sent.");
    } catch (error: unknown) {
      const axiosError = error as AxiosError<ApiErrorPayload>;
      const message =
        axiosError.response?.data?.message ||
        axiosError.message ||
        "Unable to resend verification email.";
      toast.error(message);
    } finally {
      setResending(false);
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
            <h1 className="text-2xl sm:text-3xl font-bold text-center">Verify Your Email</h1>

          {hasToken ? (
            <p className="text-center text-muted-foreground">
              {verifying
                ? "We are verifying your token..."
                : verified
                ? "Email verified. You can now sign in."
                : "Verification failed. You can request a new link below."}
            </p>
          ) : (
            <p className="text-center text-muted-foreground">
              Enter your email to resend the verification link.
            </p>
          )}

            {verified && (
              <Link to="/login" className="block slide-fade-in">
                <Button className="w-full rounded-2xl text-base sm:text-lg">Go to Login</Button>
              </Link>
            )}

          <form onSubmit={handleResend} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="verify-email">Email</Label>
              <Input
                id="verify-email"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="you@example.com"
              />
            </div>

            <Button
              type="submit"
              disabled={resending}
              className="w-full rounded-2xl text-base sm:text-lg"
              variant="outline"
            >
              {resending ? "Sending..." : "Resend verification email"}
            </Button>
          </form>
        </CardContent>
      </Card>
      </motion.div>
    </div>
  );
}

export default VerifyEmail;
