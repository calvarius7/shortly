# Multi-stage Dockerfile to build and serve the Angular app on port 80

# ===== Builder stage =====
FROM node:20-alpine AS builder

WORKDIR /app

# Install dependencies first (leverage Docker layer caching)
COPY package*.json ./

# Use npm ci when lockfile exists, otherwise fall back to npm install
RUN sh -c "if [ -f package-lock.json ]; then npm ci; else npm install; fi"

# Copy the rest of the sources
COPY . .

# Build the Angular application (prod by default)
RUN npm run build


# ===== Runtime stage =====
FROM nginx:alpine AS runtime

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built artifacts from the builder stage
# Angular 17+ default output path: dist/<project>/browser
COPY --from=builder /app/dist/shortly_angular/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
