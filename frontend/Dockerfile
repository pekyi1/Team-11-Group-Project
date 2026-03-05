# =============================================================================
# ServiceHub Frontend — Multi-Stage Dockerfile
# =============================================================================
# Stage 1: Dependencies (cached layer — only re-runs when package.json changes)
# Stage 2: Build (next build — produces optimized production output)
# Stage 3: Runtime (Node slim — serves the built app)
#
# Uses Debian-slim images (not Alpine) for cross-platform compatibility
# with both amd64 (Linux) and arm64 (Apple Silicon Mac) architectures.
# =============================================================================

# --- Stage 1: Install dependencies ---
FROM node:20-slim AS deps

WORKDIR /app

# Copy package files only — Docker caches this layer
COPY package.json package-lock.json* yarn.lock* pnpm-lock.yaml* ./

# Install dependencies based on which lockfile exists
RUN \
  if [ -f yarn.lock ]; then yarn install --frozen-lockfile; \
  elif [ -f package-lock.json ]; then npm ci; \
  elif [ -f pnpm-lock.yaml ]; then corepack enable pnpm && pnpm install --frozen-lockfile; \
  else echo "No lockfile found. Run 'npm install' first." && exit 1; \
  fi

# --- Stage 2: Build ---
FROM node:20-slim AS build

WORKDIR /app

# Copy dependencies from stage 1
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# Build argument — the API URL is baked into the client bundle at build time
# because NEXT_PUBLIC_* vars are inlined during `next build`
ARG NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL

# Disable Next.js telemetry during build
ENV NEXT_TELEMETRY_DISABLED=1

RUN npm run build

# --- Stage 3: Runtime ---
FROM node:20-slim AS runtime

WORKDIR /app

# Create non-root user
RUN groupadd -r servicehub && useradd -r -g servicehub servicehub

# Install curl for health checks
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Copy only what's needed to run (not source code or node_modules bloat)
COPY --from=build /app/public ./public
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static

# Set ownership
RUN chown -R servicehub:servicehub /app

USER servicehub

EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME="0.0.0.0"
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:3000 || exit 1

CMD ["node", "server.js"]
