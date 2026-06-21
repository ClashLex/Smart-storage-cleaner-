/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        cyber: {
          bg: "#0B0F19",
          card: "#111827",
          primary: "#10B981", // Emerald matching the organic clean green
          secondary: "#3B82F6", // Blue for telemetry
          accent: "#8B5CF6", // Purple for premium
        }
      }
    },
  },
  plugins: [],
}
