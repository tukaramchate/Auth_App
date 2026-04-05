import useAuth from "@/auth/store";
import { Spinner } from "@/components/ui/spinner";
import { refreshToken } from "@/services/AuthService";
import { useEffect, useRef, useState } from "react";
import toast from "react-hot-toast";
import { useNavigate } from "react-router";

function OAuthSuccess() {
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const started = useRef(false);

  const changeLocalLoginData = useAuth((state) => state.changeLocalLoginData);
  const navigate = useNavigate();

  useEffect(() => {
    async function getAccessToken() {
      if (started.current || isRefreshing) {
        return;
      }

      started.current = true;
      setIsRefreshing(true);
      try {
        const responseLoginData = await refreshToken();
        changeLocalLoginData(
          responseLoginData.accessToken,
          responseLoginData.user,
          true
        );

        toast.success("Login success !");
        navigate("/dashboard");
      } catch {
        toast.error("Error while login!");
      } finally {
        setIsRefreshing(false);
      }
    }

    getAccessToken();
  }, [changeLocalLoginData, isRefreshing, navigate]);

  return (
    <div className="p-10 flex flex-col gap-3 justify-center items-center">
      <Spinner />
      <h1 className="text-2xl font-semibold">Please wait....</h1>
    </div>
  );
}

export default OAuthSuccess;