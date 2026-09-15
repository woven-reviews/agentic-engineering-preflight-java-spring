import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 5173,
    // Compose reaches the dev server by service name, which Vite rejects unless listed.
    allowedHosts: ["localhost", "frontend"],
    // Docker Desktop bind mounts do not deliver inotify events into the container.
    watch: { usePolling: true },
    proxy: { "/api": process.env.VITE_API_PROXY_TARGET ?? "http://api:8080" },
  },
});
