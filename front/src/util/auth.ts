import axios from "axios";

export interface AuthUser {
    username: string;
    roles: string[];
}

export const isAuthenticated = (): boolean => {
    console.log(document.cookie);
    console.log("isAuthenticated:", document.cookie.includes("JSESSIONID"));
    return document.cookie.includes("JSESSIONID");
};

export const fetchAuth = async (): Promise<AuthUser | null> => {
    try {
      const res = await axios.get<AuthUser>('/api/auth/me', { withCredentials: true });
      console.log(res.data);
      return res.data;           // 認証済み → ユーザ情報
    } catch (err: any) {
      console.log(err);
      if (err.response?.status === 401) return null; // 未認証
      throw err;                                     // ネットワーク等は上位へ
    }
  };