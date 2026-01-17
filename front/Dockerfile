# Stage 1: Build the Angular app
FROM node:18-alpine AS build
WORKDIR /app

# Copy package files
COPY package*.json ./
COPY angular.json ./
COPY tsconfig*.json ./
COPY src/ ./src/

# Install dependencies
RUN npm ci --legacy-peer-deps

# Build the app
RUN npm run build --configuration=production

# Stage 2: Serve with Nginx
FROM nginx:alpine

# Remove default nginx static files
RUN rm -rf /usr/share/nginx/html/*

# Copy built files
COPY --from=build /app/dist/front /usr/share/nginx/html

# Copy nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]