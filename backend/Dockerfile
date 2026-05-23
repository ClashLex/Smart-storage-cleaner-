# Use standard slim Node.js LTS image
FROM node:18-slim

# Set working directory inside container
WORKDIR /usr/src/app

# Copy dependency files
COPY package*.json ./

# Install production dependencies only
RUN npm ci --only=production

# Copy application source files
COPY . .

# Expose server port
EXPOSE 3000

# Set environment
ENV NODE_ENV=production

# Start application listener
CMD ["node", "server.js"]
