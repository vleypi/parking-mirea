import type { NextConfig } from "next";
import { API_URL } from "./src/shared/config/api-url.constant";

const nextConfig: NextConfig = {
  output: "standalone",
  reactCompiler: true,
  rewrites: async () => [{ source: "/api/:path*", destination: `${API_URL}/api/:path*` }],
};

export default nextConfig;
