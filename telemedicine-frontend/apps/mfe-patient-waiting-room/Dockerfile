FROM node:20-alpine AS build
WORKDIR /app
RUN corepack enable && corepack prepare pnpm@9.15.0 --activate
COPY package.json pnpm-workspace.yaml nx.json tsconfig.base.json ./
COPY apps ./apps
COPY packages ./packages
COPY tools ./tools
RUN pnpm install --no-frozen-lockfile
RUN pnpm exec nx build mfe-patient-waiting-room

FROM nginx:1.27-alpine
COPY apps/mfe-patient-waiting-room/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/apps/mfe-patient-waiting-room /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
