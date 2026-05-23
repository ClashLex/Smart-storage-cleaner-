import React, { useState } from 'react';
import { Search, Sparkles, Filter, ChevronRight, HardDrive, Mail, Calendar } from 'lucide-react';

interface UserRecord {
  id: string;
  name: string;
  email: string;
  premiumEntitled: boolean;
  gbFreed: number;
  lastSeen: string;
  createdDate: string;
  hasAdminPrivilege: boolean;
}

// Prepopulate standard visual user list for search & filters demonstrations
const initialUsers: UserRecord[] = [
  { id: 'usr-1', name: 'Muhammed Ansil', email: 'ansilmuhammed919@gmail.com', premiumEntitled: true, gbFreed: 4.82, lastSeen: '2026-05-23 17:05', createdDate: '2026-05-15', hasAdminPrivilege: true },
  { id: 'usr-2', name: 'Leila Vance', email: 'leila.vance@gmail.com', premiumEntitled: true, gbFreed: 32.10, lastSeen: '2026-05-23 12:40', createdDate: '2026-05-01', hasAdminPrivilege: false },
  { id: 'usr-3', name: 'Devon Carter', email: 'devon.carter@tech.co', premiumEntitled: false, gbFreed: 1.25, lastSeen: '2026-05-22 09:15', createdDate: '2026-05-12', hasAdminPrivilege: false },
  { id: 'usr-4', name: 'Sarah Jenkins', email: 'sarah.j@outlook.com', premiumEntitled: true, gbFreed: 18.44, lastSeen: '2026-05-23 16:30', createdDate: '2026-04-20', hasAdminPrivilege: false },
  { id: 'usr-5', name: 'Tariq Rashid', email: 'tariq@rashid.io', premiumEntitled: false, gbFreed: 0.00, lastSeen: '2026-05-21 14:00', createdDate: '2026-05-21', hasAdminPrivilege: false },
  { id: 'usr-6', name: 'Elena Rostova', email: 'elena.rostova@yandex.com', premiumEntitled: true, gbFreed: 54.02, lastSeen: '2026-05-23 15:55', createdDate: '2026-04-05', hasAdminPrivilege: false },
  { id: 'usr-7', name: 'Chloe Fontaine', email: 'c.fontaine@orange.fr', premiumEntitled: false, gbFreed: 2.10, lastSeen: '2026-05-18 10:12', createdDate: '2026-05-08', hasAdminPrivilege: false },
  { id: 'usr-8', name: 'Marcus Brody', email: 'brody.m@indy.edu', premiumEntitled: false, gbFreed: 0.45, lastSeen: '2026-05-22 18:22', createdDate: '2026-05-19', hasAdminPrivilege: false },
  { id: 'usr-9', name: 'Akiro Tanaka', email: 'tanaka.a@sony.co.jp', premiumEntitled: true, gbFreed: 92.51, lastSeen: '2026-05-23 08:31', createdDate: '2026-03-24', hasAdminPrivilege: false },
  { id: 'usr-10', name: 'Gary Patel', email: 'gary.patel@gmail.com', premiumEntitled: false, gbFreed: 8.70, lastSeen: '2026-05-20 11:45', createdDate: '2026-05-02', hasAdminPrivilege: false },
];

interface UsersProps {
  onSelectUser: (userId: string) => void;
}

export default function Users({ onSelectUser }: UsersProps) {
  const [users, setUsers] = useState<UserRecord[]>(initialUsers);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterType, setFilterType] = useState<'all' | 'premium' | 'free' | 'admin'>('all');
  const [currentPage, setCurrentPage] = useState(1);
  const usersPerPage = 6;

  // Filter & Search logic
  const filteredUsers = users.filter((user) => {
    const matchesSearch =
      user.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.id.toLowerCase().includes(searchTerm.toLowerCase());

    const matchesFilter =
      filterType === 'all' ||
      (filterType === 'premium' && user.premiumEntitled) ||
      (filterType === 'free' && !user.premiumEntitled) ||
      (filterType === 'admin' && user.hasAdminPrivilege);

    return matchesSearch && matchesFilter;
  });

  // Pagination calculation
  const indexOfLastUser = currentPage * usersPerPage;
  const indexOfFirstUser = indexOfLastUser - usersPerPage;
  const currentUsers = filteredUsers.slice(indexOfFirstUser, indexOfLastUser);
  const totalPages = Math.ceil(filteredUsers.length / usersPerPage);

  const paginate = (pageNumber: number) => {
    setCurrentPage(pageNumber);
  };

  return (
    <div className="space-y-6 animate-fade-in pb-12 w-full">
      <div className="border-b border-slate-800 pb-5">
        <h1 className="text-3xl font-bold text-white tracking-tight">User Registrars</h1>
        <p className="text-gray-400 text-sm mt-1">
          Search accounts, audit subscription entitlements, and inspect cumulative local storage gains.
        </p>
      </div>

      {/* Filter and search bar layout */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-slate-900/60 border border-slate-800 p-4 rounded-2xl">
        <div className="relative flex-1">
          <span className="absolute inset-y-0 left-0 flex items-center pl-3.5 pointer-events-none">
            <Search className="h-4.5 w-4.5 text-slate-400" />
          </span>
          <input
            type="text"
            placeholder="Search accounts by name, email, or user identifier..."
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value);
              setCurrentPage(1); // reset to page 1 on active search
            }}
            className="w-full bg-slate-950 border border-slate-800 text-white rounded-xl py-2.5 pl-11 pr-4 text-sm focus:border-emerald-500/50 focus:outline-none transition-all placeholder:text-slate-500"
          />
        </div>

        {/* Custom filter buttons */}
        <div className="flex items-center gap-2 overflow-x-auto shrink-0 select-none">
          <div className="flex items-center gap-1.5 text-xs text-slate-400 mr-2 border-r border-slate-800 pr-3.5">
            <Filter className="h-3.5 w-3.5" />
            <span>Class Filter:</span>
          </div>

          <button
            onClick={() => { setFilterType('all'); setCurrentPage(1); }}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              filterType === 'all'
                ? 'bg-slate-800 text-white border border-slate-700'
                : 'text-slate-400 hover:text-white border border-transparent'
            }`}
          >
            All Accounts ({users.length})
          </button>

          <button
            onClick={() => { setFilterType('premium'); setCurrentPage(1); }}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-1 ${
              filterType === 'premium'
                ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'
                : 'text-slate-400 hover:text-white border border-transparent'
            }`}
          >
            <Sparkles className="h-3.5 w-3.5 text-emerald-400" />
            Premium
          </button>

          <button
            onClick={() => { setFilterType('free'); setCurrentPage(1); }}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              filterType === 'free'
                ? 'bg-slate-800 text-white border border-slate-700'
                : 'text-slate-400 hover:text-white border border-transparent'
            }`}
          >
            Standard Free
          </button>

          <button
            onClick={() => { setFilterType('admin'); setCurrentPage(1); }}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              filterType === 'admin'
                ? 'bg-purple-500/10 text-purple-400 border border-purple-500/30'
                : 'text-slate-400 hover:text-white border border-transparent'
            }`}
          >
            Administrators
          </button>
        </div>
      </div>

      {/* Grid containing accounts list table */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-2xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-950/40 text-xs font-bold text-slate-400 tracking-wider">
                <th className="py-4.5 px-6">Account/Security Profile</th>
                <th className="py-4.5 px-6">Email / Channel</th>
                <th className="py-4.5 px-6">Moneyness Tier</th>
                <th className="py-4.5 px-6 text-right">Disk Space Freed</th>
                <th className="py-4.5 px-6 text-center">Last Handshake</th>
                <th className="py-4.5 px-6">Joined Date</th>
                <th className="py-4.5 px-6 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800 text-sm">
              {currentUsers.length > 0 ? (
                currentUsers.map((user) => (
                  <tr
                    key={user.id}
                    onClick={() => onSelectUser(user.id)}
                    className="hover:bg-slate-800/40 cursor-pointer transition-all border-b border-slate-800/40"
                  >
                    <td className="py-4.5 px-6">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-xl bg-slate-950 flex items-center justify-center font-bold text-slate-300 border border-slate-850">
                          {user.name.split(' ').map((n) => n[0]).join('')}
                        </div>
                        <div>
                          <p className="font-semibold text-white flex items-center gap-1.5">
                            {user.name}
                            {user.hasAdminPrivilege && (
                              <span className="text-[10px] bg-purple-500/15 text-purple-400 px-1.5 py-0.5 rounded-md font-mono border border-purple-500/20">
                                ADMIN
                              </span>
                            )}
                          </p>
                          <p className="text-[11px] text-gray-400 font-mono mt-0.5">{user.id}</p>
                        </div>
                      </div>
                    </td>
                    <td className="py-4.5 px-6 text-slate-300">
                      <div className="flex items-center gap-1.5">
                        <Mail className="h-3.5 w-3.5 text-slate-500 shrink-0" />
                        <span>{user.email}</span>
                      </div>
                    </td>
                    <td className="py-4.5 px-6">
                      {user.premiumEntitled ? (
                        <span className="inline-flex items-center gap-1 pl-1.5 pr-2.5 py-0.5 bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 rounded-full text-xs font-bold leading-none">
                          <Sparkles className="h-3 w-3 text-emerald-400" />
                          Premium Entitled
                        </span>
                      ) : (
                        <span className="inline-flex items-center px-2.5 py-0.5 bg-slate-950 text-slate-500 border border-slate-800 rounded-full text-xs font-medium leading-none">
                          Standard Clean
                        </span>
                      )}
                    </td>
                    <td className="py-4.5 px-6 text-right font-mono font-bold text-white">
                      <div className="inline-flex items-center gap-1">
                        <HardDrive className="h-3.5 w-3.5 text-slate-500" />
                        <span>{user.gbFreed.toFixed(2)} GB</span>
                      </div>
                    </td>
                    <td className="py-4.5 px-6 text-center text-slate-400 font-mono text-xs">
                      {user.lastSeen}
                    </td>
                    <td className="py-4.5 px-6 text-slate-400 text-xs">
                      <div className="flex items-center gap-1">
                        <Calendar className="h-3.5 w-3.5 text-slate-550" />
                        <span>{user.createdDate}</span>
                      </div>
                    </td>
                    <td className="py-4.5 px-6 text-right" onClick={(e) => e.stopPropagation()}>
                      <button
                        onClick={() => onSelectUser(user.id)}
                        className="p-1.5 hover:bg-slate-950/60 rounded-lg text-slate-400 hover:text-white transition-all border border-transparent hover:border-slate-800"
                      >
                        <ChevronRight className="h-4.5 w-4.5" />
                      </button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={7} className="text-center py-12 text-slate-500">
                    No active user records matched search queries.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Elegant pagination footer standard */}
        {totalPages > 1 && (
          <div className="bg-slate-950/40 border-t border-slate-800 px-6 py-4 flex items-center justify-between text-xs">
            <span className="text-slate-400">
              Showing <span className="text-white font-bold">{indexOfFirstUser + 1}</span> to{' '}
              <span className="text-white font-bold">
                {Math.min(indexOfLastUser, filteredUsers.length)}
              </span>{' '}
              of <span className="text-white font-bold">{filteredUsers.length}</span> administrators
            </span>
            <div className="flex items-center gap-1">
              <button
                disabled={currentPage === 1}
                onClick={() => paginate(currentPage - 1)}
                className="px-2.5 py-1.5 rounded-lg border border-slate-800 text-slate-400 hover:text-white disabled:opacity-40 select-none font-bold"
              >
                Previous
              </button>
              {Array.from({ length: totalPages }).map((_, id) => (
                <button
                  key={id}
                  onClick={() => paginate(id + 1)}
                  className={`w-8 h-8 rounded-lg font-bold leading-none ${
                    currentPage === id + 1
                      ? 'bg-emerald-500 text-slate-950 font-extrabold'
                      : 'border border-slate-800 text-slate-400 hover:text-white'
                  }`}
                >
                  {id + 1}
                </button>
              ))}
              <button
                disabled={currentPage === totalPages}
                onClick={() => paginate(currentPage + 1)}
                className="px-2.5 py-1.5 rounded-lg border border-slate-800 text-slate-400 hover:text-white disabled:opacity-40 select-none font-bold"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
