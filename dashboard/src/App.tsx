import React, { useState, useEffect } from 'react';
import { subscribeToAuthChanges, logoutUser } from './firebase';
import Login from './pages/Login';
import Overview from './pages/Overview';
import Users from './pages/Users';
import UserDetail from './pages/UserDetail';
import Subscriptions from './pages/Subscriptions';
import Funnel from './pages/Funnel';
import Retention from './pages/Retention';
import { 
  BarChart2, 
  Users as UsersIcon, 
  CreditCard, 
  Filter, 
  Calendar, 
  LogOut, 
  ShieldCheck, 
  Menu, 
  X,
  HardDrive
} from 'lucide-react';

type PageId = 'overview' | 'users' | 'subscriptions' | 'funnel' | 'retention';

export default function App() {
  const [currentUser, setCurrentUser] = useState<any>(null);
  const [authToken, setAuthToken] = useState<string>('');
  const [loading, setLoading] = useState(true);
  
  // Navigation & detailed drill-down state variables
  const [activePage, setActivePage] = useState<PageId>('overview');
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [sidebarOpen, setSidebarOpen] = useState(false);

  // Monitor Auth lifecycle
  useEffect(() => {
    const unsubscribe = subscribeToAuthChanges(async (user) => {
      if (user) {
        try {
          const token = await user.getIdToken();
          setAuthToken(token);
          
          // Decode or mock user matching
          setCurrentUser({
            uid: user.uid,
            email: user.email,
            displayName: user.displayName || 'Authorized Admin',
            photoURL: user.photoURL || 'https://api.dicebear.com/7.x/bottts/svg?seed=ansil'
          });
        } catch (_) {
          setCurrentUser(null);
        }
      } else {
        setCurrentUser(null);
        setAuthToken('');
      }
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const handleLoginSuccess = (userId: string, token: string, userDetails: any) => {
    setAuthToken(token);
    setCurrentUser(userDetails);
  };

  const handleLogout = async () => {
    if (confirm("Sign out of active admin session?")) {
      await logoutUser();
      setCurrentUser(null);
      setAuthToken('');
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-[#070B14] flex flex-col items-center justify-center text-slate-450 gap-4">
        <div className="w-10 h-10 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin"></div>
        <p className="text-sm font-mono text-emerald-400">Loading Administrative Shell...</p>
      </div>
    );
  }

  // Not signed in? Render login board
  if (!currentUser) {
    return <Login onLoginSuccess={handleLoginSuccess} />;
  }

  const renderActivePage = () => {
    if (selectedUserId) {
      return (
        <UserDetail 
          userId={selectedUserId} 
          onBack={() => setSelectedUserId(null)} 
          token={authToken}
        />
      );
    }

    switch (activePage) {
      case 'overview':
        return <Overview token={authToken} />;
      case 'users':
        return <Users onSelectUser={(id) => setSelectedUserId(id)} />;
      case 'subscriptions':
        return <Subscriptions token={authToken} />;
      case 'funnel':
        return <Funnel />;
      case 'retention':
        return <Retention />;
    }
  };

  const menuItems = [
    { id: 'overview', name: 'Overview', icon: BarChart2 },
    { id: 'users', name: 'Users', icon: UsersIcon },
    { id: 'subscriptions', name: 'Subscriptions', icon: CreditCard },
    { id: 'funnel', name: 'Funnel', icon: Filter },
    { id: 'retention', name: 'Retention', icon: Calendar },
  ];

  return (
    <div className="min-h-screen bg-[#070B14] text-slate-100 flex relative overflow-x-hidden">
      {/* Sidebar background overlay on mobile view */}
      {sidebarOpen && (
        <div 
          onClick={() => setSidebarOpen(false)}
          className="lg:hidden fixed inset-0 bg-slate-950/60 z-40 backdrop-blur-sm"
        ></div>
      )}

      {/* Main Admin Sidebar Scaffold */}
      <aside className={`
        fixed lg:static top-0 bottom-0 left-0 w-64 bg-slate-900 border-r border-slate-800 z-50 flex flex-col justify-between transition-transform duration-300
        ${sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
      `}>
        <div className="flex flex-col">
          {/* Logo container */}
          <div className="h-16 flex items-center justify-between px-6 border-b border-slate-850">
            <span className="flex items-center gap-2">
              <span className="p-1.5 bg-emerald-500/10 border border-emerald-500/30 rounded-lg">
                <HardDrive className="h-5 w-5 text-emerald-400" />
              </span>
              <p className="font-bold text-white text-sm">
                Smart Cleaner <span className="text-emerald-400 font-mono">AI</span>
              </p>
            </span>
            <button 
              onClick={() => setSidebarOpen(false)}
              className="lg:hidden p-1.5 hover:bg-slate-800 rounded-lg text-slate-400"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          {/* Navigation Links list */}
          <nav className="p-4 space-y-1.5">
            {menuItems.map((item) => {
              const active = activePage === item.id && !selectedUserId;
              return (
                <button
                  key={item.id}
                  onClick={() => {
                    setActivePage(item.id as PageId);
                    setSelectedUserId(null); // clear drill downs on tab switch
                    setSidebarOpen(false);
                  }}
                  className={`
                    w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-semibold transition-all select-none
                    ${active 
                      ? 'bg-emerald-500 text-slate-950 shadow-md shadow-emerald-500/5' 
                      : 'text-slate-450 hover:bg-slate-850 hover:text-white'
                    }
                  `}
                >
                  <item.icon className={`h-5 w-5 ${active ? 'text-slate-950' : 'text-slate-450'}`} />
                  <span>{item.name}</span>
                </button>
              );
            })}
          </nav>
        </div>

        {/* Active Administrator Identity details */}
        <div className="p-4 border-t border-slate-850 space-y-4">
          <div className="flex items-center gap-3">
            <img 
              src={currentUser.photoURL} 
              alt="Avatar" 
              className="w-10 h-10 rounded-xl bg-slate-950 border border-slate-800"
              onError={(e) => {
                (e.target as HTMLImageElement).src = 'https://api.dicebear.com/7.x/bottts/svg?seed=ansil';
              }}
            />
            <div className="min-w-0 flex-1">
              <p className="text-sm font-bold text-white truncate">{currentUser.displayName}</p>
              <p className="text-[10px] text-zinc-500 font-mono truncate">{currentUser.email}</p>
            </div>
          </div>

          <button
            onClick={handleLogout}
            className="w-full flex items-center justify-center gap-2 border border-slate-800 hover:border-red-500/20 hover:bg-red-500/5 hover:text-red-400 text-slate-400 font-semibold py-2.5 px-4 rounded-xl text-xs transition-all select-none cursor-pointer"
          >
            <LogOut className="h-4 w-4" />
            <span>Sign Out Session</span>
          </button>
        </div>
      </aside>

      {/* Main Content viewport panel */}
      <div className="flex-1 flex flex-col min-w-0 bg-[#070B14]">
        {/* App Top Toolbar (Header) */}
        <header className="h-16 border-b border-slate-850 flex items-center justify-between px-6 lg:px-8 select-none bg-slate-900/40 backdrop-blur-md">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setSidebarOpen(true)}
              className="lg:hidden p-2 hover:bg-slate-900 border border-slate-800 rounded-xl text-slate-300"
            >
              <Menu className="h-5 w-5" />
            </button>
            <div className="flex items-center gap-2">
              <ShieldCheck className="h-5 w-5 text-emerald-400" />
              <span className="text-xs font-mono font-bold text-slate-400">Node: server_primary_cluster_asia</span>
            </div>
          </div>

          <div className="flex items-center gap-1.5 opacity-60">
            <span className="h-2 w-2 rounded-full bg-emerald-400 animate-ping"></span>
            <span className="text-[11px] font-mono text-slate-400 font-medium">Synced GMT-0:00</span>
          </div>
        </header>

        {/* Viewport scrolling page view Container */}
        <main className="flex-1 overflow-y-auto p-6 md:p-8">
          <div className="max-w-6xl mx-auto w-full">
            {renderActivePage()}
          </div>
        </main>
      </div>
    </div>
  );
}
