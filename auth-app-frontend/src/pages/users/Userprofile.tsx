import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { motion } from "framer-motion";
import useAuth from "@/auth/store";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import type { AxiosError } from "axios";
import type Session from "@/models/Session";
import {
  getUserSessions,
  revokeAllCurrentUserSessions,
  revokeCurrentUserSession,
  updateCurrentUserProfile,
} from "@/services/AuthService";
import toast from "react-hot-toast";

type ApiErrorDetail = {
  field?: string;
  reason?: string;
};

type ApiErrorResponse = {
  message?: string;
  details?: ApiErrorDetail[];
};

function Userprofile() {
  const [isEditing, setIsEditing] = useState(false);
  const [sessions, setSessions] = useState<Session[]>([]);
  const [loadingSessions, setLoadingSessions] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [formValues, setFormValues] = useState({ name: "", image: "" });
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const user = useAuth((state) => state.user);
  const updateCurrentUser = useAuth((state) => state.updateCurrentUser);
  const navigate = useNavigate();

  useEffect(() => {
    setFormValues({
      name: user?.name ?? "",
      image: user?.image ?? "",
    });
  }, [user?.name, user?.image]);

  useEffect(() => {
    const loadSessions = async () => {
      setLoadingSessions(true);
      try {
        const response = await getUserSessions();
        setSessions(response);
      } catch {
        toast.error("Failed to load sessions");
      } finally {
        setLoadingSessions(false);
      }
    };

    loadSessions();
  }, []);

  const enabledValue = (user?.enable ?? user?.enabled) ? "Yes" : "No";

  const parseApiError = (error: unknown) => {
    const axiosError = error as AxiosError<ApiErrorResponse>;
    const responseData = axiosError.response?.data;
    const nextErrors: Record<string, string> = {};

    (responseData?.details ?? []).forEach((detail) => {
      if (detail?.field && detail?.reason) {
        nextErrors[detail.field] = detail.reason;
      }
    });

    return {
      message: responseData?.message || "Failed to update profile",
      fieldErrors: nextErrors,
    };
  };

  const startEditing = () => {
    setFieldErrors({});
    setFormValues({
      name: user?.name ?? "",
      image: user?.image ?? "",
    });
    setIsEditing(true);
  };

  const cancelEditing = () => {
    setFieldErrors({});
    setFormValues({
      name: user?.name ?? "",
      image: user?.image ?? "",
    });
    setIsEditing(false);
  };

  const saveProfile = async () => {
    setIsSaving(true);
    setFieldErrors({});
    try {
      const updatedUser = await updateCurrentUserProfile({
        name: formValues.name.trim(),
        image: formValues.image.trim() ? formValues.image.trim() : undefined,
      });
      updateCurrentUser(updatedUser);
      toast.success("Profile updated successfully");
      setIsEditing(false);
    } catch (error) {
      const parsed = parseApiError(error);
      setFieldErrors(parsed.fieldErrors);
      toast.error(parsed.message);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="px-4 py-6 sm:p-6 max-w-3xl mx-auto space-y-8">
      <motion.h1
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        className="text-2xl sm:text-3xl font-bold text-center"
      >
        User Profile
      </motion.h1>

      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
      >
      <Card className="rounded-2xl shadow-md p-4 sm:p-6 bg-card/80 backdrop-blur-lg">
        <CardHeader>
          <CardTitle className="text-xl font-semibold">Profile Information</CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="flex flex-col items-center gap-3">
            <Avatar className="w-28 h-28 border shadow-md">
              <AvatarImage src={user?.image || "https://api.dicebear.com/7.x/thumbs/svg?seed=user"} />
              <AvatarFallback>U</AvatarFallback>
            </Avatar>
            <p className="text-sm text-muted-foreground">Update your profile details below.</p>
          </div>

          {!isEditing ? (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="name">Full Name</Label>
                <Input id="name" value={user?.name} readOnly className="rounded-xl" />
              </div>
              <div className="space-y-2">
                <Label htmlFor="image">Profile Image URL</Label>
                <Input id="image" value={user?.image ?? ""} readOnly className="rounded-xl" />
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input id="email" value={user?.email} readOnly className="rounded-xl" />
              </div>
              <div className="space-y-2">
                <Label htmlFor="provider">Provider</Label>
                <Input id="provider" value={user?.provider} readOnly className="rounded-xl" />
              </div>
              <div className="space-y-2">
                <Label htmlFor="enabled">Enabled</Label>
                <Input id="enabled" value={enabledValue} readOnly className="rounded-xl" />
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="name">Full Name</Label>
                <Input
                  id="name"
                  value={formValues.name}
                  onChange={(event) =>
                    setFormValues((current) => ({ ...current, name: event.target.value }))
                  }
                  className="rounded-xl"
                  disabled={isSaving}
                />
                {fieldErrors.name && <p className="text-sm text-red-500">{fieldErrors.name}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="image">Profile Image URL</Label>
                <Input
                  id="image"
                  value={formValues.image}
                  onChange={(event) =>
                    setFormValues((current) => ({ ...current, image: event.target.value }))
                  }
                  className="rounded-xl"
                  disabled={isSaving}
                  placeholder="https://example.com/avatar.png"
                />
                {fieldErrors.image && <p className="text-sm text-red-500">{fieldErrors.image}</p>}
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input id="email" value={user?.email} readOnly className="rounded-xl" />
              </div>
              <div className="space-y-2">
                <Label htmlFor="provider">Provider</Label>
                <Input id="provider" value={user?.provider} readOnly className="rounded-xl" />
              </div>
              <div className="space-y-2">
                <Label htmlFor="enabled">Enabled</Label>
                <Input id="enabled" value={enabledValue} readOnly className="rounded-xl" />
              </div>
            </div>
          )}

          {!isEditing ? (
            <Button onClick={startEditing} className="w-full rounded-2xl mt-4 text-lg">
              Edit Profile
            </Button>
          ) : (
            <div className="flex flex-col sm:flex-row gap-3 mt-4">
              <Button className="rounded-2xl w-full" onClick={cancelEditing} disabled={isSaving}>
                Cancel
              </Button>
              <Button className="rounded-2xl w-full" onClick={saveProfile} disabled={isSaving}>
                {isSaving ? "Saving..." : "Save"}
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45, delay: 0.1 }}
      >
      <Card className="rounded-2xl shadow-md p-4 sm:p-6 bg-card/80 backdrop-blur-lg">
        <CardHeader>
          <CardTitle className="text-xl">Account Settings</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <Button
            variant="outline"
            className="w-full rounded-xl py-3 text-base"
            onClick={() => navigate("/forgot-password")}
          >
            Change Password
          </Button>
          <Button variant="destructive" className="w-full rounded-xl py-3 text-base">
            Delete Account
          </Button>
        </CardContent>
      </Card>
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.45, delay: 0.15 }}
      >
      <Card className="rounded-2xl shadow-md p-4 sm:p-6 bg-card/80 backdrop-blur-lg">
        <CardHeader>
          <CardTitle className="text-xl">Active Sessions</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {loadingSessions ? (
            <p className="text-sm text-muted-foreground">Loading sessions...</p>
          ) : sessions.length === 0 ? (
            <p className="text-sm text-muted-foreground">No active sessions found.</p>
          ) : (
            sessions.map((session) => (
              <div
                key={session.id}
                className="rounded-xl border p-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"
              >
                <div>
                  <p className="font-medium">{session.deviceLabel ?? "Session"}</p>
                  <p className="text-sm text-muted-foreground">{session.userAgent ?? "Unknown browser"}</p>
                  <p className="text-xs text-muted-foreground">IP: {session.ipAddress ?? "Unknown"}</p>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-xs text-muted-foreground">
                    {session.revoked ? "Revoked" : "Active"}
                  </span>
                  <Button
                    variant="outline"
                    className="rounded-xl"
                    onClick={async () => {
                      try {
                        await revokeCurrentUserSession(session.jti);
                        toast.success("Session revoked");
                        setSessions((current) => current.filter((item) => item.jti !== session.jti));
                      } catch {
                        toast.error("Could not revoke session");
                      }
                    }}
                    disabled={session.revoked}
                  >
                    Revoke
                  </Button>
                </div>
              </div>
            ))
          )}

          <Button
            variant="destructive"
            className="w-full rounded-xl py-3 text-base"
            onClick={async () => {
              try {
                await revokeAllCurrentUserSessions();
                toast.success("All sessions revoked");
                setSessions([]);
              } catch {
                toast.error("Could not revoke sessions");
              }
            }}
          >
            Revoke All Sessions
          </Button>
        </CardContent>
      </Card>
      </motion.div>
    </div>
  );
}

export default Userprofile;