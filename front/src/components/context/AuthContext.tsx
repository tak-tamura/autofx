// src/contexts/AuthContext.tsx
import React, { createContext, useState, useContext, useEffect, ReactNode } from 'react';

// ユーザー情報のインターフェース
export interface User {
  username: string;
  authorities: string[];
}

// コンテキストで提供する型
interface AuthContextType {
  user: User | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);

  // ログイン処理
  const login = async (username: string, password: string) => {
    const body = new URLSearchParams({ username, password });
    const res = await fetch('/login', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: body,
    });
    if (!res.ok) {
      const text = await res.text();
      throw new Error(text || 'ログインに失敗しました');
    }
    // セッション Cookie がセット
    // 認証後にユーザー情報を取得
    const meRes = await fetch('/api/auth/me', { credentials: 'include' });
    if (!meRes.ok) throw new Error('ユーザー情報の取得に失敗しました');
    const data = (await meRes.json()) as User;
    setUser(data);
    localStorage.setItem('user', JSON.stringify(data));
  };

  // ログアウト処理
  const logout = async () => {
    await fetch('/logout', {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
    });
    setUser(null);
    localStorage.removeItem('user');
  };

  // 初期化: LocalStorage またはセッションから情報を取得
  useEffect(() => {
    const saved = localStorage.getItem('user');
    if (saved) {
      setUser(JSON.parse(saved));
    } else {
      fetch('/api/auth/me', { credentials: 'include' })
        .then(res => {
          if (!res.ok) throw new Error();
          return res.json();
        })
        .then((data: User) => setUser(data))
        .catch(() => {
            setUser(null);
        });
    }
  }, []);

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

// カスタムフック
export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};