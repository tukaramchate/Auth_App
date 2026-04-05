import useAuth from "@/auth/store";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type User from "@/models/User";
import {
  createUserByAdmin,
  deleteUserByAdmin,
  getAllUsersByAdmin,
  getSystemRoles,
  updateUserByAdmin,
} from "@/services/AuthService";
import type { AxiosError } from "axios";
import { useCallback, useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";

type NewUserForm = {
  name: string;
  email: string;
  password: string;
};

type UserDraft = {
  enabled: boolean;
  role: string;
};

const initialForm: NewUserForm = {
  name: "",
  email: "",
  password: "",
};

function AdminDashboard() {
  const user = useAuth((state) => state.user);
  const [users, setUsers] = useState<User[]>([]);
  const [roles, setRoles] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [savingUserId, setSavingUserId] = useState<string | null>(null);
  const [form, setForm] = useState<NewUserForm>(initialForm);
  const [drafts, setDrafts] = useState<Record<string, UserDraft>>({});
  const [page, setPage] = useState(0);
  const [pageSize] = useState(20);
  const [search, setSearch] = useState("");
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);

  const totalUsers = users.length;
  const enabledUsers = useMemo(
    () => users.filter((item) => item.enable ?? item.enabled).length,
    [users]
  );
  const adminUsers = useMemo(
    () =>
      users.filter((item) =>
        (item.roles ?? []).some((role) => role.name === "ADMIN")
      ).length,
    [users]
  );

  const loadUsers = useCallback(async (targetPage = page) => {
    setLoading(true);
    try {
      const [response, roleList] = await Promise.all([
        getAllUsersByAdmin({ page: targetPage, size: pageSize, q: search || undefined }),
        getSystemRoles(),
      ]);
      setUsers(response.items);
      setPage(response.page);
      setTotalPages(response.totalPages);
      setTotalItems(response.totalItems);
      setRoles(roleList);
      setDrafts(
        Object.fromEntries(
          response.items.map((item) => [item.id, {
            enabled: item.enable ?? item.enabled ?? false,
            role: item.roles?.[0]?.name ?? "USER",
          }])
        )
      );
    } catch {
      toast.error("Failed to load users");
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, search]);

  useEffect(() => {
    loadUsers(0);
  }, [loadUsers]);

  const handleCreateUser = async () => {
    if (!form.name.trim() || !form.email.trim() || !form.password.trim()) {
      toast.error("Please fill all fields");
      return;
    }

    setCreating(true);
    try {
      await createUserByAdmin({
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password,
        enabled: true,
      });
      toast.success("User created");
      setForm(initialForm);
      await loadUsers(page);
    } catch {
      toast.error("Could not create user");
    } finally {
      setCreating(false);
    }
  };

  const handleDeleteUser = async (targetUser: User) => {
    if (!targetUser.id) {
      return;
    }

    if (targetUser.email === user?.email) {
      toast.error("You cannot delete your own account here");
      return;
    }

    const confirmed = window.confirm(
      `Delete user ${targetUser.email}? This cannot be undone.`
    );
    if (!confirmed) {
      return;
    }

    try {
      await deleteUserByAdmin(targetUser.id);
      toast.success("User deleted");
      await loadUsers(page);
    } catch (error) {
      const axiosError = error as AxiosError<{ message?: string }>;
      const message = axiosError.response?.data?.message || "Could not delete user";
      toast.error(message);
    }
  };

  const handleSaveUser = async (targetUser: User) => {
    if (!targetUser.id) {
      return;
    }

    const draft = drafts[targetUser.id];
    if (!draft) {
      return;
    }

    setSavingUserId(targetUser.id);
    try {
      const updated = await updateUserByAdmin(targetUser.id, {
        enabled: draft.enabled,
        roles: [{ name: draft.role }],
      });
      setUsers((current) => current.map((item) => (item.id === updated.id ? updated : item)));
      toast.success("User updated");
    } catch {
      toast.error("Could not update user");
    } finally {
      setSavingUserId(null);
    }
  };

  return (
    <div className="mx-auto w-full max-w-6xl px-4 py-6 sm:px-6">
      <h1 className="mb-6 text-3xl font-bold">Admin Dashboard</h1>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>User Search</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <div className="w-full sm:max-w-sm">
            <Label htmlFor="search-user">Search by name or email</Label>
            <Input
              id="search-user"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search users"
            />
          </div>
          <Button onClick={() => loadUsers(0)}>Apply</Button>
        </CardContent>
      </Card>

      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle>Total Users</CardTitle>
          </CardHeader>
          <CardContent className="text-3xl font-semibold">{totalUsers}</CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Enabled Users</CardTitle>
          </CardHeader>
          <CardContent className="text-3xl font-semibold">{enabledUsers}</CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Admin Users</CardTitle>
          </CardHeader>
          <CardContent className="text-3xl font-semibold">{adminUsers}</CardContent>
        </Card>
      </div>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Create User</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            <div>
              <Label htmlFor="new-name">Name</Label>
              <Input
                id="new-name"
                value={form.name}
                onChange={(event) =>
                  setForm((prev) => ({ ...prev, name: event.target.value }))
                }
                placeholder="User full name"
              />
            </div>
            <div>
              <Label htmlFor="new-email">Email</Label>
              <Input
                id="new-email"
                type="email"
                value={form.email}
                onChange={(event) =>
                  setForm((prev) => ({ ...prev, email: event.target.value }))
                }
                placeholder="user@example.com"
              />
            </div>
            <div>
              <Label htmlFor="new-password">Password</Label>
              <Input
                id="new-password"
                type="password"
                value={form.password}
                onChange={(event) =>
                  setForm((prev) => ({ ...prev, password: event.target.value }))
                }
                placeholder="Temporary password"
              />
            </div>
          </div>
          <Button
            className="mt-4"
            onClick={handleCreateUser}
            disabled={creating}
          >
            {creating ? "Creating..." : "Create User"}
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>User Management</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <p className="text-sm text-muted-foreground">Loading users...</p>
          ) : (
            <div className="overflow-x-auto space-y-4">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b">
                    <th className="py-3">Name</th>
                    <th className="py-3">Email</th>
                    <th className="py-3">Provider</th>
                    <th className="py-3">Roles</th>
                    <th className="py-3">Status</th>
                    <th className="py-3">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((item) => (
                    <tr key={item.id} className="border-b last:border-0">
                      <td className="py-3">{item.name || "-"}</td>
                      <td className="py-3">{item.email}</td>
                      <td className="py-3">{item.provider || "-"}</td>
                      <td className="py-3">
                        <select
                          className="rounded-md border bg-background px-2 py-1"
                          value={drafts[item.id]?.role ?? item.roles?.[0]?.name ?? "USER"}
                          onChange={(event) =>
                            setDrafts((current) => ({
                              ...current,
                              [item.id]: {
                                enabled: current[item.id]?.enabled ?? (item.enable ?? item.enabled ?? false),
                                role: event.target.value,
                              },
                            }))
                          }
                        >
                          {roles.map((role) => (
                            <option key={role} value={role}>
                              {role}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td className="py-3">
                        <label className="flex items-center gap-2">
                          <input
                            type="checkbox"
                            checked={drafts[item.id]?.enabled ?? (item.enable ?? item.enabled ?? false)}
                            onChange={(event) =>
                              setDrafts((current) => ({
                                ...current,
                                [item.id]: {
                                  enabled: event.target.checked,
                                  role: current[item.id]?.role ?? item.roles?.[0]?.name ?? "USER",
                                },
                              }))
                            }
                          />
                          <span>{(drafts[item.id]?.enabled ?? (item.enable ?? item.enabled ?? false)) ? "Enabled" : "Disabled"}</span>
                        </label>
                      </td>
                      <td className="py-3">
                        <div className="flex gap-2">
                        <Button
                          size="sm"
                          onClick={() => handleSaveUser(item)}
                          disabled={savingUserId === item.id}
                        >
                          {savingUserId === item.id ? "Saving..." : "Save"}
                        </Button>
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => handleDeleteUser(item)}
                        >
                          Delete
                        </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="flex items-center justify-between gap-3">
                <p className="text-xs text-muted-foreground">
                  Showing page {page + 1} of {Math.max(totalPages, 1)} ({totalItems} users)
                </p>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page <= 0 || loading}
                    onClick={() => loadUsers(page - 1)}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page + 1 >= totalPages || loading}
                    onClick={() => loadUsers(page + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

export default AdminDashboard;
