import React, { useState } from 'react';
import { loginWithGoogle, isMockMode } from '../firebase';
import { ShieldAlert, LogIn, HardDrive, Cpu } from 'lucide-react';

interface LoginProps {
  onLoginSuccess: (userId: string, token: string, userDetails: any) => void;
}

export default function Login({ onLoginSuccess }: LoginProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleLogin = async () => {
    setLoading(true);
    setError(null);
    try {
      const user = await loginWithGoogle();
      const token = await user.getIdToken();

      // Exchange Firebase Token with the Backend API /api/auth/sync
      const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:3000';
      
      const response = await fetch(`${apiUrl}/api/auth/sync`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
      });

      if (!response.ok) {
        throw new Error(`Authentication synchronization failed: ${response.statusText}`);
      }

      const data = await response.json();

      // Check for Admin status before admitting entry to Dashboard
      if (data.user.role !== 'admin' && !isMockMode) {
        setError("Access Denied. Custom claims validation failed: You are not registered as an administrator.");
        setLoading(false);
        return;
      }

      onLoginSuccess(data.user.id, data.token, data.user);
    } catch (err: any) {
      console.error(err);
      if (isMockMode) {
        // Mock fallback succeed completely
        onLoginSuccess('mock_user_id', 'mock_jwt_token', {
          id: 'mock_user_id',
          email: 'admin@smartcleaner.ai',
          displayName: 'Ansil Admin Mode',
          premiumEntitled: true,
          role: 'admin'
        });
      } else {
        setError(err.message || "Failed to log in with Google.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#070B14] p-6 relative overflow-hidden">
      {/* Decorative ambient blurred backing rings */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-emerald-500/10 rounded-full filter blur-[100px] pointer-events-none"></div>
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-blue-500/10 rounded-full filter blur-[100px] pointer-events-none"></div>

      <div className="w-full max-w-md bg-slate-900/80 border border-slate-800 rounded-2xl p-8 backdrop-blur-xl shadow-2xl relative z-10">
        <div className="flex flex-col items-center mb-8">
          <div className="w-16 h-16 bg-emerald-500/10 rounded-2xl flex items-center justify-center border border-emerald-500/30 mb-4 animate-pulse">
            <HardDrive className="h-8 w-8 text-emerald-400" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
            Smart Storage <span className="text-emerald-400 font-mono">Cleaner AI</span>
          </h1>
          <p className="text-gray-400 text-sm mt-1 text-center">
            Central Administrative Intelligence Platform
          </p>
        </div>

        {error && (
          <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-4 rounded-xl flex gap-3 text-sm mb-6">
            <ShieldAlert className="h-5 w-5 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <div className="space-y-4">
          <p className="text-xs text-gray-400 leading-relaxed text-center mb-4">
            Authorized Personnel Only. Actions logged in chronological session telemetry logs.
          </p>

          <button
            onClick={handleLogin}
            disabled={loading}
            className="w-full bg-emerald-500 hover:bg-emerald-600 active:scale-[0.98] transition-all text-slate-950 font-semibold py-3.5 px-6 rounded-xl flex items-center justify-center gap-3 disabled:opacity-50"
          >
            {loading ? (
              <Cpu className="h-5 w-5 animate-spin text-slate-950" />
            ) : (
              <LogIn className="h-5 w-5 text-slate-950" />
            )}
            {loading ? "Authorizing Security..." : "Sign in as Administrator"}
          </button>

          {isMockMode && (
            <div className="mt-6 flex flex-col items-center border-t border-slate-800/60 pt-4">
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-ping"></span>
                Local Admin Sandbox Mode active
              </span>
              <p className="text-[11px] text-gray-500 text-center mt-2 leading-relaxed">
                No real Firebase credentials found. Direct sandbox login bypass enabled using simulated identities.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
