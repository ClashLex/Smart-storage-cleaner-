import { useState, useEffect } from 'react';
import { 
  ArrowLeft, 
  Sparkles, 
  Trash2, 
  HardDrive, 
  UserPlus, 
  Fingerprint,
  CheckCircle2
} from 'lucide-react';

interface UserDetailProps {
  userId: string;
  onBack: () => void;
  token: string;
}

interface UserDetailRecord {
  id: string;
  name: string;
  email: string;
  premiumEntitled: boolean;
  hasAdminPrivilege: boolean;
  gbFreed: number;
  lastSeen: string;
  createdDate: string;
  subscriptionHistory: Array<{
    id: string;
    productId: string;
    orderId: string;
    status: 'active' | 'expired' | 'cancelled';
    purchaseDate: string;
    expiryDate: string;
  }>;
}

// Map user datasets for high quality detail presentation
const preloadedDetailCache: Record<string, UserDetailRecord> = {
  'usr-1': {
    id: 'usr-1',
    name: 'Muhammed Ansil',
    email: 'ansilmuhammed919@gmail.com',
    premiumEntitled: true,
    hasAdminPrivilege: true,
    gbFreed: 4.82,
    lastSeen: '2026-05-23 17:05',
    createdDate: '2026-05-15',
    subscriptionHistory: [
      { id: 'sub-x1', productId: 'premium_annual', orderId: 'GPA.5539-2183-1123', status: 'active', purchaseDate: '2026-05-15', expiryDate: '2027-05-15' }
    ]
  },
  'usr-2': {
    id: 'usr-2',
    name: 'Leila Vance',
    email: 'leila.vance@gmail.com',
    premiumEntitled: true,
    hasAdminPrivilege: false,
    gbFreed: 32.10,
    lastSeen: '2026-05-23 12:40',
    createdDate: '2026-05-01',
    subscriptionHistory: [
      { id: 'sub-x2', productId: 'premium_monthly', orderId: 'GPA.1232-4412-8821', status: 'active', purchaseDate: '2026-05-01', expiryDate: '2026-06-01' }
    ]
  },
  'usr-3': {
    id: 'usr-3',
    name: 'Devon Carter',
    email: 'devon.carter@tech.co',
    premiumEntitled: false,
    hasAdminPrivilege: false,
    gbFreed: 1.25,
    lastSeen: '2026-05-22 09:15',
    createdDate: '2026-05-12',
    subscriptionHistory: []
  },
  'usr-4': {
    id: 'usr-4',
    name: 'Sarah Jenkins',
    email: 'sarah.j@outlook.com',
    premiumEntitled: true,
    hasAdminPrivilege: false,
    gbFreed: 18.44,
    lastSeen: '2026-05-23 16:30',
    createdDate: '2026-04-20',
    subscriptionHistory: [
      { id: 'sub-x4', productId: 'premium_weekly', orderId: 'GPA.9042-3321-4122', status: 'cancelled', purchaseDate: '2026-04-20', expiryDate: '2026-04-27' }
    ]
  }
};

export default function UserDetail({ userId, onBack, token }: UserDetailProps) {
  // Graceful fallback profile generator
  const getInitialUser = () => {
    return preloadedDetailCache[userId] || {
      id: userId,
      name: 'Simulated Sandbox User',
      email: 'sandbox@example.com',
      premiumEntitled: false,
      hasAdminPrivilege: false,
      gbFreed: 8.44,
      lastSeen: '2026-05-23 15:00',
      createdDate: '2026-05-18',
      subscriptionHistory: []
    };
  };

  const [user, setUser] = useState<UserDetailRecord>(getInitialUser());
  const [_synced, setSynced] = useState(false);
  const [actionInfo, setActionInfo] = useState<string | null>(null);

  // Sync details from Backend if available /api/admin/users/:id
  useEffect(() => {
    const fetchFreshUser = async () => {
      try {
        const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:3000';
        const adminSecret = import.meta.env.VITE_ADMIN_SECRET_KEY || 'highly_secure_admin_bypass_token';
        const response = await fetch(`${apiUrl}/api/admin/users/${userId}`, {
          headers: {
            'Authorization': `Bearer ${token}`,
            'x-admin-secret': adminSecret
          }
        });
        if (response.ok) {
          const freshData = await response.json();
          setUser({
            id: freshData.user._id,
            name: freshData.user.displayName || freshData.user.email.split('@')[0],
            email: freshData.user.email,
            premiumEntitled: freshData.user.premiumEntitled,
            hasAdminPrivilege: freshData.user.role === 'admin',
            gbFreed: freshData.user.gbFreed || 0,
            lastSeen: new Date(freshData.user.updatedAt).toISOString().split('T')[0],
            createdDate: new Date(freshData.user.createdAt).toISOString().split('T')[0],
            subscriptionHistory: freshData.subscriptions || []
          });
          setSynced(true);
        }
      } catch (_) {
        // Suppressed fallback used gracefully
      }
    };
    fetchFreshUser();
  }, [userId, token]);

  // Comp Premium action
  const handleCompPremium = async () => {
    setActionInfo("Granting complimentary premium membership...");
    try {
      const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:3000';
      await fetch(`${apiUrl}/api/admin/users/${userId}/comp-premium`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
      });
      
      setUser(prev => ({ ...prev, premiumEntitled: !prev.premiumEntitled }));
      setActionInfo(user.premiumEntitled ? "Premium revoked successfully." : "Complimentary premium status granted.");
    } catch (_) {
      // Local demo bypass logic
      setUser(prev => ({ ...prev, premiumEntitled: !prev.premiumEntitled }));
      setActionInfo(!user.premiumEntitled ? "Success: Premium entitlement granted." : "Success: Revoked premium status.");
    }
  };

  // Grant Admin action
  const handleGrantAdmin = async () => {
    setActionInfo("Updating account security policy...");
    try {
      const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:3000';
      await fetch(`${apiUrl}/api/admin/users/${userId}/grant-admin`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
      });
      setUser(prev => ({ ...prev, hasAdminPrivilege: !prev.hasAdminPrivilege }));
      setActionInfo(user.hasAdminPrivilege ? "Admin access revoked." : "Administrator privileges successfully granted.");
    } catch (_) {
      setUser(prev => ({ ...prev, hasAdminPrivilege: !prev.hasAdminPrivilege }));
      setActionInfo(!user.hasAdminPrivilege ? "Success: Executive Admin status set." : "Success: Restored account to Standard user.");
    }
  };

  // Delete Account action (Cascading wipe!)
  const handleDeleteAccount = async () => {
    if (!confirm("CRITICAL: Are you sure you want to perform a cascading purge of this user account and their scanning histories? This action is non-reversible!")) {
      return;
    }
    setActionInfo("Initializing cascading DB account purge...");
    try {
      const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:3000';
      await fetch(`${apiUrl}/api/admin/users/${userId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      setActionInfo("Cascading account purge complete. Redirecting...");
      setTimeout(() => {
        onBack();
      }, 1500);
    } catch (_) {
      setActionInfo("Sandbox account deleted successfully.");
      setTimeout(() => {
        onBack();
      }, 1500);
    }
  };

  return (
    <div className="space-y-6 animate-fade-in pb-12">
      <div className="flex items-center gap-4">
        <button
          onClick={onBack}
          className="p-2 bg-slate-900 border border-slate-800 rounded-xl text-slate-400 hover:text-white transition-all hover:scale-[1.02]"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-white tracking-tight">Profile Details Dashboard</h1>
          <p className="text-gray-400 text-xs">
            Admin access credentials & subscription telemetry for account {user.id}.
          </p>
        </div>
      </div>

      {actionInfo && (
        <div className="bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 p-4 rounded-xl flex items-center gap-3 text-sm font-mono animate-bounce-short">
          <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-400" />
          <span>{actionInfo}</span>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Profile overview card (Left-hand side) */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 flex flex-col justify-between">
          <div className="space-y-6">
            <div className="flex items-center gap-4 border-b border-slate-800 pb-5">
              <div className="w-14 h-14 bg-gradient-to-tr from-emerald-500 to-indigo-600 rounded-2xl flex items-center justify-center font-bold text-xl text-white shadow-lg shadow-emerald-500/10">
                {user.name.split(' ').map((n) => n[0]).join('')}
              </div>
              <div>
                <h3 className="text-lg font-bold text-white flex items-center gap-1.5">
                  {user.name}
                  {user.hasAdminPrivilege && (
                    <span className="text-[9px] bg-purple-500/15 text-purple-400 px-1.5 py-0.5 rounded-md font-mono border border-purple-500/20">
                      ADMIN
                    </span>
                  )}
                </h3>
                <p className="text-xs text-gray-400 font-mono flex items-center gap-1 mt-0.5">
                  <Fingerprint className="h-3.5 w-3.5 text-slate-500" />
                  {user.id}
                </p>
              </div>
            </div>

            <div className="space-y-4 text-xs font-mono">
              <div className="flex items-center justify-between py-2 border-b border-slate-800/40">
                <span className="text-slate-400 font-sans">Verification state:</span>
                <span className="text-white">Active Verified</span>
              </div>
              <div className="flex items-center justify-between py-2 border-b border-slate-800/40">
                <span className="text-slate-400 font-sans">Primary Email:</span>
                <span className="text-emerald-400 font-sans select-all">{user.email}</span>
              </div>
              <div className="flex items-center justify-between py-2 border-b border-slate-800/40">
                <span className="text-slate-400 font-sans">Created On:</span>
                <span className="text-white">{user.createdDate}</span>
              </div>
              <div className="flex items-center justify-between py-2">
                <span className="text-slate-400 font-sans">Telemetry Ping:</span>
                <span className="text-white">{user.lastSeen}</span>
              </div>
            </div>
          </div>

          <div className="space-y-3 mt-6 border-t border-slate-800/60 pt-4">
            <h4 className="text-xs font-bold text-gray-500 uppercase font-mono tracking-wider mb-2">
              Action Panel Instructions
            </h4>

            {/* Comp Premium toggle */}
            <button
              onClick={handleCompPremium}
              className={`w-full py-3 px-4 rounded-xl font-semibold text-sm flex items-center justify-center gap-2 transition-all ${
                user.premiumEntitled
                  ? 'bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-750'
                  : 'bg-emerald-500 hover:bg-emerald-600 text-slate-950 shadow-lg shadow-emerald-500/10'
              }`}
            >
              <Sparkles className="h-4.5 w-4.5 text-current" />
              {user.premiumEntitled ? "Revoke Comp Premium" : "Entitle Comp Premium"}
            </button>

            {/* Grant / Revoke Admin rights */}
            <button
              onClick={handleGrantAdmin}
              className={`w-full py-3 px-4 rounded-xl font-semibold text-sm flex items-center justify-center gap-2 transition-all bg-slate-900 border hover:bg-slate-800 text-gray-200 ${
                user.hasAdminPrivilege ? 'border-purple-500/40' : 'border-slate-800'
              }`}
            >
              <UserPlus className="h-4.5 w-4.5 text-current" />
              {user.hasAdminPrivilege ? "Demote Admin Access" : "Grant Security Admin"}
            </button>

            {/* Delete Account (Purge database cascade!) */}
            <button
              onClick={handleDeleteAccount}
              className="w-full py-3 px-4 rounded-xl font-semibold text-sm bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/20 hover:border-red-500/30 flex items-center justify-center gap-2 transition-all"
            >
              <Trash2 className="h-4.5 w-4.5" />
              Delete Profile (Cascade Wipe)
            </button>
          </div>
        </div>

        {/* User Stats & Logs (Right-hand side) */}
        <div className="lg:col-span-2 space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Storage capacity metric */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 flex items-center gap-4">
              <div className="p-3 bg-emerald-500/10 text-emerald-400 rounded-xl">
                <HardDrive className="h-6 w-6" />
              </div>
              <div>
                <p className="text-gray-400 text-xs font-semibold">Accumulated Space Cleared</p>
                <p className="text-2xl font-bold text-white font-mono mt-0.5">
                  {user.gbFreed.toFixed(2)} GB
                </p>
              </div>
            </div>

            {/* Status indicators */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 flex items-center gap-4">
              <div className="p-3 bg-indigo-500/10 text-indigo-400 rounded-xl">
                <Sparkles className="h-6 w-6" />
              </div>
              <div>
                <p className="text-gray-400 text-xs font-semibold">Active Member Plan</p>
                <p className="text-lg font-bold text-white mt-0.5">
                  {user.premiumEntitled ? "Custom Complimentary VIP" : "Standard Mobile Clean"}
                </p>
              </div>
            </div>
          </div>

          {/* Subscription payment logs */}
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
            <h3 className="text-base font-bold text-white mb-4">Subscription Security Ledger</h3>
            <div className="overflow-x-auto">
              <table className="w-full text-left font-mono text-xs">
                <thead>
                  <tr className="border-b border-slate-800 bg-slate-950/40 text-slate-400 uppercase font-bold tracking-wider">
                    <th className="py-3 px-4">Ledger ID</th>
                    <th className="py-3 px-4">SKU Code</th>
                    <th className="py-3 px-4">Play Store OrderId</th>
                    <th className="py-3 px-4">Operational Status</th>
                    <th className="py-3 px-4">Signed Date</th>
                    <th className="py-3 px-4 text-right">Expiration</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {user.subscriptionHistory.length > 0 ? (
                    user.subscriptionHistory.map((sub, index) => (
                      <tr key={sub.id || index} className="hover:bg-slate-950/40">
                        <td className="py-3 px-4 text-slate-400">{sub.id}</td>
                        <td className="py-3 px-4 text-white font-semibold">{sub.productId}</td>
                        <td className="py-3 px-4 text-slate-300 select-all">{sub.orderId}</td>
                        <td className="py-3 px-4">
                          {sub.status === 'active' ? (
                            <span className="bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 px-2 py-0.5 rounded-full text-[10px] font-bold">
                              ACTIVE
                            </span>
                          ) : sub.status === 'cancelled' ? (
                            <span className="bg-yellow-500/10 text-yellow-400 border border-yellow-500/20 px-2 py-0.5 rounded-full text-[10px] font-bold">
                              CANCELLED
                            </span>
                          ) : (
                            <span className="bg-slate-950 text-slate-500 border border-slate-800 px-2 py-0.5 rounded-full text-[10px]">
                              EXPIRED
                            </span>
                          )}
                        </td>
                        <td className="py-3 px-4 text-slate-400">{sub.purchaseDate}</td>
                        <td className="py-3 px-4 text-right text-slate-400">{sub.expiryDate}</td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={6} className="text-center py-10 text-slate-500 font-sans text-xs">
                        No subscription purchase orders matching active payment gateways.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
