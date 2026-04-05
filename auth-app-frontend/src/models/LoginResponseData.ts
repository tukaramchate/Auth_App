import type User from "./User";

export default interface LoginResponseData {
  accessToken: string;
  user: User;
  expiresIn: number;
  tokenType: string;
}