import React from 'react';
import { Calendar, HelpCircle, ArrowRight } from 'lucide-react';

interface CohortRow {
  cohort: string;
  size: number;
  w0: number; // week 0 (always 100%)
  w1: number;
  w2: number;
  w3: number;
  w4: number;
  w5: number;
  w6: number;
  w7: number;
  w8: number;
}

const cohortRetentionData: CohortRow[] = [
  { cohort: 'Apr 04 - Apr 10', size: 1240, w0: 100, w1: 42, w2: 35, w3: 28, w4: 25, w5: 22, w6: 18, w7: 15, w8: 12 },
  { cohort: 'Apr 11 - Apr 17', size: 1380, w0: 100, w1: 45, w2: 38, w3: 31, w4: 29, w5: 24, w6: 21, w7: 17, w8: 14 },
  { cohort: 'Apr 18 - Apr 24', size: 1420, w0: 100, w1: 48, w2: 41, w3: 36, w4: 32, w5: 28, w6: 24, w7: 19, w8: 0 },
  { cohort: 'Apr 25 - May 01', size: 1510, w0: 100, w1: 52, w2: 45, w3: 39, w4: 35, w5: 31, w6: 27, w7: 0, w8: 0 },
  { cohort: 'May 02 - May 08', size: 1680, w0: 100, w1: 56, w2: 49, w3: 42, w4: 38, w5: 34, w6: 0, w7: 0, w8: 0 },
  { cohort: 'May 09 - May 15', size: 1750, w0: 100, w1: 58, w2: 52, w3: 46, w4: 41, w5: 0, w6: 0, w7: 0, w8: 0 },
  { cohort: 'May 16 - May 22', size: 1920, w0: 100, w1: 61, w2: 54, w3: 48, w4: 0, w5: 0, w6: 0, w7: 0, w8: 0 },
  { cohort: 'May 23 - May 29', size: 1480, w0: 100, w1: 65, w2: 0, w3: 0, w4: 0, w5: 0, w6: 0, w7: 0, w8: 0 },
];

// Resolves a color map gradient representation based on percentage values
const getRetentionColorClass = (val: number, isPlaceholder: boolean) => {
  if (isPlaceholder) return 'bg-slate-950 text-slate-700 border-slate-900 border';
  if (val === 100) return 'bg-emerald-600/30 text-emerald-300 font-extrabold border border-emerald-500/20';
  if (val >= 50) return 'bg-emerald-500/20 text-emerald-400 font-bold border border-emerald-500/10';
  if (val >= 35) return 'bg-teal-500/15 text-teal-400 border border-teal-500/10';
  if (val >= 25) return 'bg-indigo-500/15 text-indigo-400 border border-indigo-500/10';
  if (val >= 15) return 'bg-slate-800 text-slate-400 border border-slate-700/50';
  return 'bg-red-500/10 text-red-400/80 border border-red-500/10';
};

export default function Retention() {
  return (
    <div className="space-y-6 animate-fade-in pb-12 w-full">
      <div className="border-b border-slate-800 pb-5">
        <h1 className="text-3xl font-bold text-white tracking-tight">Weekly N-Day Retention Cohort Matrix</h1>
        <p className="text-gray-400 text-sm mt-1">
          Tracking the recurring scanning density of weekly cohorts to observe product-market fit.
        </p>
      </div>

      {/* Cohort Grid Container */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-2xl space-y-6 overflow-hidden">
        <div className="flex items-center justify-between border-b border-slate-800 pb-4">
          <div className="flex items-center gap-2">
            <Calendar className="h-5 w-5 text-indigo-400" />
            <h3 className="font-bold text-white text-base">Weekly User Scan Operations Retention Matrix</h3>
          </div>
          <div className="flex items-center gap-3 text-xs text-slate-400">
            <span className="flex items-center gap-1">
              <span className="h-2.5 w-2.5 bg-emerald-500/20 border border-emerald-500/30 rounded"></span>
              High (&ge;50%)
            </span>
            <span className="flex items-center gap-1">
              <span className="h-2.5 w-2.5 bg-red-500/10 border border-red-500/20 rounded"></span>
              Low (&lt;15%)
            </span>
          </div>
        </div>

        {/* Responsive horizontal scroll wrap */}
        <div className="overflow-x-auto">
          <table className="w-full text-center border-collapse text-xs font-mono">
            <thead>
              <tr className="border-b border-slate-800 text-slate-400 font-bold uppercase tracking-wider text-[11px]">
                <th className="py-3 px-4 text-left font-sans">Cohort Register Range</th>
                <th className="py-3 px-4 font-sans">Cohort Size</th>
                <th className="py-3 px-2">Wk 0</th>
                <th className="py-3 px-2">Wk 1</th>
                <th className="py-3 px-2">Wk 2</th>
                <th className="py-3 px-2">Wk 3</th>
                <th className="py-3 px-2">Wk 4</th>
                <th className="py-3 px-2">Wk 5</th>
                <th className="py-3 px-2">Wk 6</th>
                <th className="py-3 px-2">Wk 7</th>
                <th className="py-3 px-2">Wk 8</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {cohortRetentionData.map((row, idx) => (
                <tr key={idx} className="hover:bg-slate-850/20 transition-colors">
                  <td className="py-4 px-4 text-left font-sans text-white font-semibold">
                    {row.cohort}
                  </td>
                  <td className="py-4 px-4 font-sans text-slate-300 font-bold bg-slate-950/20">
                    {row.size.toLocaleString()}
                  </td>
                  
                  {/* Cohort Columns cells with dynamic color scale class helpers */}
                  <td className={`py-4 px-2 ${getRetentionColorClass(row.w0, false)}`}>
                    {row.w0}%
                  </td>
                  <td className={`py-4 px-2 ${getRetentionColorClass(row.w1, row.w1 === 0)}`}>
                    {row.w1 > 0 ? `${row.w1}%` : '-'}
                  </td>
                  <td className={`py-4 px-2 ${getRetentionColorClass(row.w2, row.w2 === 0)}`}>
                    {row.w2 > 0 ? `${row.w2}%` : '-'}
                  </td>
                  <td className={`py-4 px-2 ${getRetentionColorClass(row.w3, row.w3 === 0)}`}>
                    {row.w3 > 0 ? `${row.w3}%` : '-'}
                  </td>
                  <td className={`py-4 px-2 ${getRetentionColorClass(row.w4, row.w4 === 0)}`}>
                    {row.w4 > 0 ? `${row.w4}%` : '-'}
                  </td>
                  <td className={`py-4 px-2 ${getRetentionColorClass(row.w5, row.w5 === 0)}`}>
                    {row.w5 > 0 ? `${row.w5}%` : '-'}
                  </td>
                  <td className={`py-4 px-2 ${getRetentionColorClass(row.w6, row.w6 === 0)}`}>
                    {row.w6 > 0 ? `${row.w6}%` : '-'}
                  </td>
                  <td className={`py-4 px-2 ${getRetentionColorClass(row.w7, row.w7 === 0)}`}>
                    {row.w7 > 0 ? `${row.w7}%` : '-'}
                  </td>
                  <td className={`py-4 px-2 ${getRetentionColorClass(row.w8, row.w8 === 0)}`}>
                    {row.w8 > 0 ? `${row.w8}%` : '-'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Cohort interpretations analysis */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
        <h3 className="text-base font-bold text-white mb-2">Cohort Interpretative Insights</h3>
        <p className="text-xs text-slate-400 leading-relaxed">
          Retention displays an organic upward trend following the deployment of version 1.2 on <span className="text-emerald-400 font-bold">May 16th</span>. The Wk 1 active retend level rose from 42% to 61% because of the faster cache scanning algorithm inside the Android framework codebase, eliminating user dropoff rates early.
        </p>
      </div>
    </div>
  );
}
