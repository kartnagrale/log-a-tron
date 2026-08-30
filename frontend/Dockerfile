FROM node:22.20.0-alpine AS build
WORKDIR /app
RUN corepack enable && corepack prepare pnpm@11.19.0 --activate
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile
COPY angular.json tsconfig*.json ./
COPY public public
COPY src src
RUN pnpm build

FROM nginxinc/nginx-unprivileged:1.29.1-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/web-ui/browser /usr/share/nginx/html
EXPOSE 8080

