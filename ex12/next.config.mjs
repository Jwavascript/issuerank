/** @type {import('next').NextConfig} */
const nextConfig = {
  async rewrites() {
    return [{ source: '/', destination: '/index.html' }, { source: '/widget', destination: '/widget.html' }];
  },
};

export default nextConfig;
