import type { NextConfig } from "next";

const backendUrl = process.env.BACKEND_API_URL || "http://localhost:8081";

const nextConfig: NextConfig = {
  devIndicators: false,
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${backendUrl}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
