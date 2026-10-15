FROM node:20-alpine AS build
WORKDIR /app
RUN corepack enable && corepack prepare pnpm@9.15.0 --activate
COPY package.json pnpm-workspace.yaml nx.json tsconfig.base.json ./
COPY apps ./apps
COPY packages ./packages
COPY tools ./tools
RUN pnpm install --no-frozen-lockfile
WORKDIR /app/apps/shell
RUN pnpm exec vite build

FROM nginx:1.27-alpine
COPY apps/shell/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/apps/shell /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
