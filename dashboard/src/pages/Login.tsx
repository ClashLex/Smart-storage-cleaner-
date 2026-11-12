import { useState } from 'react';
import { loginWithGoogle } from '../firebase';
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
      const apiUrl = import.meta.env.VITE_API_URL;

      if (!apiUrl) throw new Error('VITE_API_URL is not configured.');

      const response = await fetch(`${apiUrl.replace(/\/$/, '')}/api/auth/sync`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(response.status === 403
          ? 'Access denied. This account does not have administrator privileges.'
          : `Authentication synchronization failed (HTTP ${response.status}).`);
      }

      const data = await response.json();
      if (data.user?.role !== 'admin') {
        throw new Error('Access denied. This account is not registered as an administrator.');
      }

      onLoginSuccess(data.user.id, data.token, data.user);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to log in with Google.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#070B14] p-6 relative overflow-hidden">
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-emerald-500/10 rounded-full filter blur-[100px] pointer-events-none"></div>
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-blue-500/10 rounded-full filter blur-[100px] pointer-events-none"></div>
      <div className="w-full max-w-md bg-slate-900/80 border border-slate-800 rounded-2xl p-8 backdrop-blur-xl shadow-2xl relative z-10">
        <div className="flex flex-col items-center mb-8">
          <div className="w-16 h-16 bg-emerald-500/10 rounded-2xl flex items-center justify-center border border-emerald-500/30 mb-4">
            <HardDrive className="h-8 w-8 text-emerald-400" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-white flex items-center gap-2">
            Smart Storage <span className="text-emerald-400 font-mono">Cleaner AI</span>
          </h1>
          <p className="text-gray-400 text-sm mt-1 text-center">Central Administrative Intelligence Platform</p>
        </div>
        {error && (
          <div role="alert" className="bg-red-500/10 border border-red-500/20 text-red-400 p-4 rounded-xl flex gap-3 text-sm mb-6">
            <ShieldAlert className="h-5 w-5 shrink-0" />
            <span>{error}</span>
          </div>
        )}
        <div className="space-y-4">
          <p className="text-xs text-gray-400 leading-relaxed text-center mb-4">
            Authorized Personnel Only. Administrator privileges are verified by the backend.
          </p>
          <button onClick={handleLogin} disabled={loading}
            className="w-full bg-emerald-500 hover:bg-emerald-600 active:scale-[0.98] transition-all text-slate-950 font-semibold py-3.5 px-6 rounded-xl flex items-center justify-center gap-3 disabled:opacity-50">
            {loading ? <Cpu className="h-5 w-5 animate-spin" /> : <LogIn className="h-5 w-5" />}
            {loading ? 'Authorizing...' : 'Sign in as Administrator'}
          </button>
        </div>
      </div>
    </div>
  );
}
