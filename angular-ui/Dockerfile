# syntax=docker/dockerfile:1
# Build from the angular-ui directory:
#   docker build -t ecommerce-ui angular-ui

FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# The production config replaces environment.ts with environment.prod.ts, whose
# apiUrl points at the gateway published on the host (http://localhost:8080).
RUN npm run build -- --configuration production

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/ecommerce-ui/browser /usr/share/nginx/html
EXPOSE 80
