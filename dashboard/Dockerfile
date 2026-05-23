# Stage 1: Build the React Application
FROM node:18-slim AS builder
WORKDIR /usr/src/app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Serve Built Files via Nginx Slim
FROM nginx:alpine
COPY --from=builder /usr/src/app/dist /usr/share/nginx/html

# Custom Nginx configuration to support client-side routing fallback (for react-router)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
