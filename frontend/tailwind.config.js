export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      fontFamily: {
        heading: ["Sora", "sans-serif"],
        body: ["Space Grotesk", "sans-serif"]
      },
      colors: {
        ink: "#111827",
        mist: "#eef2ff",
        accent: "#0f766e",
        warm: "#f59e0b",
        danger: "#dc2626"
      },
      keyframes: {
        revealUp: {
          "0%": { opacity: "0", transform: "translateY(12px)" },
          "100%": { opacity: "1", transform: "translateY(0)" }
        },
        pulseEdge: {
          "0%, 100%": { strokeDashoffset: "12" },
          "50%": { strokeDashoffset: "0" }
        }
      },
      animation: {
        revealUp: "revealUp 700ms ease forwards",
        pulseEdge: "pulseEdge 1800ms linear infinite"
      }
    }
  },
  plugins: []
};
