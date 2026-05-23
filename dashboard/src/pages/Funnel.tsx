import React from 'react';
import { Download, UserCheck, Search, Trash2, ShieldCheck, ArrowDown, TrendingDown } from 'lucide-react';

interface FunnelStep {
  stage: string;
  volume: number;
  icon: React.ComponentType<any>;
  color: string;
  description: string;
}

const funnelData: FunnelStep[] = [
  { stage: 'App Store Installs', volume: 45000, icon: Download, color: 'bg-blue-500', description: 'Total application downloads from Google Play console store pages.' },
  { stage: 'Account Signups', volume: 14820, icon: UserCheck, color: 'bg-indigo-500', description: 'Users completing Google Sign-In synchronization setup.' },
  { stage: 'Performed Scans', volume: 11200, icon: Search, color: 'bg-purple-500', description: 'Accounts finishing at least one local smart space analysis run.' },
  { stage: 'Executed Cleanups', volume: 8400, icon: Trash2, color: 'bg-pink-500', description: 'Schedules releasing storage by wiping duplicate files and caches.' },
  { stage: 'Paid Subscriptions', volume: 2140, icon: ShieldCheck, color: 'bg-emerald-500', description: 'Active members subscribing to premium monthly/annual SKUs.' },
];

export default function Funnel() {
  const maxVolume = funnelData[0].volume;

  return (
    <div className="space-y-6 animate-fade-in pb-12 w-full">
      <div className="border-b border-slate-800 pb-5">
        <h1 className="text-3xl font-bold text-white tracking-tight">Lifecycle Funnel</h1>
        <p className="text-gray-400 text-sm mt-1">
          Visualizing user acquisition metrics, scanning conversions, and subscription checkout elasticity.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Visual Funnel (Left Columns) */}
        <div className="lg:col-span-2 space-y-4">
          {funnelData.map((step, idx) => {
            const widthPct = (step.volume / maxVolume) * 100;
            const previousStep = idx > 0 ? funnelData[idx - 1] : null;
            const dropoffPct = previousStep 
              ? ((previousStep.volume - step.volume) / previousStep.volume * 100).toFixed(1) 
              : null;
            
            const conversionFromStart = ((step.volume / maxVolume) * 100).toFixed(1);

            return (
              <div key={idx} className="space-y-3">
                {/* Visual Connector / Dropoff block */}
                {dropoffPct && (
                  <div className="flex justify-center items-center py-1 select-none">
                    <div className="flex items-center gap-1.5 px-3 py-1 bg-red-500/10 border border-red-500/20 rounded-full text-[10px] font-bold text-red-400">
                      <TrendingDown className="h-3.5 w-3.5" />
                      <span>-{dropoffPct}% Dropoff</span>
                    </div>
                  </div>
                )}

                <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 hover:border-slate-700 transition-all">
                  <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-3">
                    <div className="flex items-center gap-3">
                      <div className={`p-2.5 rounded-xl ${step.color} text-slate-950 shadow-md`}>
                        <step.icon className="h-5 w-5" />
                      </div>
                      <div>
                        <h3 className="font-bold text-white text-base leading-none">{step.stage}</h3>
                        <p className="text-xs text-gray-400 mt-1.5 leading-relaxed font-sans">{step.description}</p>
                      </div>
                    </div>

                    <div className="text-right font-mono self-start md:self-center shrink-0">
                      <p className="text-lg font-bold text-white leading-none">{step.volume.toLocaleString()}</p>
                      <p className="text-xs text-slate-400 mt-1 leading-none">
                        {idx === 0 ? "Baseline Head" : `${conversionFromStart}% Overall`}
                      </p>
                    </div>
                  </div>

                  {/* Visual funnel percentage slice */}
                  <div className="w-full bg-slate-950 rounded-full h-3 overflow-hidden">
                    <div 
                      className={`h-full rounded-full transition-all duration-1000 ${step.color} bg-gradient-to-r from-transparent to-white/20`}
                      style={{ width: `${widthPct}%` }}
                    ></div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Executive Conversion Diagnostics (Right-hand statistics panel) */}
        <div className="space-y-6">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
            <h3 className="text-lg font-bold text-white mb-4">Diagnostics Metrics</h3>
            <div className="space-y-4">
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800/60 font-mono">
                <p className="text-slate-500 text-[10px] uppercase font-bold">Acquisition efficiency</p>
                <p className="text-2xl font-bold text-white mt-1">32.9%</p>
                <p className="text-xs text-slate-400 mt-1.5 font-sans leading-relaxed">
                  Percentage of storefront installs completing email registration syncing.
                </p>
              </div>

              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800/60 font-mono">
                <p className="text-slate-500 text-[10px] uppercase font-bold">Paid subscription yield</p>
                <p className="text-2xl font-bold text-emerald-400 mt-1">19.1%</p>
                <p className="text-xs text-slate-400 mt-1.5 font-sans leading-relaxed">
                  Percentage of active cleaning accounts unlocking VIP premium membership.
                </p>
              </div>

              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800/60 font-mono">
                <p className="text-slate-500 text-[10px] uppercase font-bold">Terminal LTV Conversion</p>
                <p className="text-2xl font-bold text-indigo-400 mt-1">4.75%</p>
                <p className="text-xs text-slate-400 mt-1.5 font-sans leading-relaxed">
                  Total cumulative yield from base store traffic to paying users.
                </p>
              </div>
            </div>
          </div>

          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
            <h3 className="text-sm font-bold text-white mr-2">Optimizations & AI Insights</h3>
            <p className="text-xs text-gray-400 leading-relaxed mt-2">
              Our models predict that reducing duplicate analysis latency inside the Jetpack code by 300ms will elevate scan conversion rates by approximately <span className="text-emerald-400 font-bold">4.2%</span>.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
