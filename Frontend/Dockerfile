# ---- V12 - The Definitive Build with Correct Network Binding ----

# ---- Stage 1: The Builder ----
FROM node:18-alpine AS builder
WORKDIR /app
ENV NEXT_TELEMETRY_DISABLED 1
COPY src/frontend/package*.json ./
RUN npm install --timeout=600000
COPY src/frontend/ ./
RUN rm -rf ./cypress
RUN npm run build

# ---- Stage 2: The Production Runner ----
FROM node:18-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

ENV NODE_ENV production
ENV NEXT_TELEMETRY_DISABLED 1
ENV PORT 3000

# --- THIS IS THE FINAL FIX ---
# This tells the Next.js server to listen on all network interfaces,
# not just localhost. This is required for it to be accessible from outside the container.
ENV HOSTNAME 0.0.0.0

# Copy the essential, optimized artifacts from the builder stage
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/static ./.next/static

EXPOSE 3000
CMD ["node", "server.js"]
