# ServiceHub Frontend

A modern, responsive web application built with Next.js 14 for the ServiceHub internal service request and ticketing system. The frontend provides an intuitive interface for users to submit, track, and manage service requests with full SLA tracking and role-based access control.

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Key Features](#key-features)
- [Authentication Flow](#authentication-flow)
- [API Integration](#api-integration)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Available Scripts](#available-scripts)
- [Component Library](#component-library)
- [Pages](#pages)
- [Contributing](#contributing)

## Overview

ServiceHub is an internal ticketing platform designed for enterprise service request management. The frontend application connects to a Spring Boot REST API and provides:

- **User Authentication**: Secure JWT-based login and registration with token refresh
- **Role-Based Access Control**: Different dashboards and permissions for USER, AGENT, and ADMIN roles
- **Request Management**: Create, view, filter, and track service requests
- **Real-Time Metrics**: Dashboard with SLA compliance, status breakdown, and request statistics
- **Modern UI/UX**: Clean, accessible interface with responsive design

## Tech Stack

| Category | Technology |
|----------|------------|
| Framework | Next.js 14 (App Router) |
| Language | TypeScript |
| UI Library | React 18 |
| Styling | Tailwind CSS 4 |
| Components | shadcn/ui (Radix UI primitives) |
| Forms | React Hook Form + Zod |
| Icons | Lucide React |
| Notifications | Sonner |
| State Management | React Context API |

## Project Structure

```
frontend/
├── src/
│   ├── app/                    # Next.js App Router pages
│   │   ├── (auth)/             # Authentication route group
│   │   │   ├── login/          # Login page
│   │   │   └── register/       # Registration page
│   │   ├── dashboard/          # Protected dashboard routes
│   │   │   ├── page.tsx        # Main dashboard overview
│   │   │   ├── layout.tsx      # Dashboard layout with sidebar
│   │   │   └── requests/       # Requests listing page
│   │   ├── request-form/       # Standalone request form
│   │   ├── globals.css         # Global styles
│   │   ├── layout.tsx          # Root layout
│   │   ├── middleware.ts       # Route protection middleware
│   │   └── page.tsx            # Root redirect page
│   └── schema/
│       └── form.tsx            # Zod form validation schemas
├── components/
│   ├── dashboard/              # Dashboard-specific components
│   │   ├── CreateTicketModal.tsx   # Modal for creating new requests
│   │   ├── RequestsTable.tsx       # Paginated requests table
│   │   └── Sidebar.tsx             # Navigation sidebar
│   └── ui/                    # Reusable UI component library
│       ├── button.tsx
│       ├── card.tsx
│       ├── dialog.tsx
│       ├── field.tsx
│       ├── input.tsx
│       ├── input-group.tsx
│       ├── label.tsx
│       ├── separator.tsx
│       └── textarea.tsx
├── lib/
│   ├── api.ts                 # Centralized API client with auth
│   ├── auth.ts                # Authentication utilities
│   ├── AuthContext.tsx        # React Context for auth state
│   └── utils.ts               # Utility functions (cn helper)
├── data/
│   └── menu.tsx               # Navigation menu configuration
├── .env                       # Environment variables
├── Dockerfile                 # Docker containerization
├── next.config.js             # Next.js configuration
├── package.json               # Dependencies and scripts
├── tsconfig.json              # TypeScript configuration
└── postcss.config.mjs         # PostCSS configuration
```

## Key Features

### Authentication System

- **JWT Token Management**: Access tokens (1 hour expiry) and refresh tokens (7 days expiry)
- **Automatic Token Refresh**: Transparent token rotation on 401 responses
- **Secure Storage**: Tokens stored in localStorage with cookie synchronization for middleware
- **Role-Based Routing**: Automatic redirect to role-specific dashboards
- **Session Persistence**: User state maintained across page refreshes

### Dashboard

- **Metrics Overview**: Real-time statistics including:
  - Total requests count
  - Open requests
  - In-progress requests
  - SLA breached count
  - SLA compliance percentage
- **Status Breakdown**: Visual breakdown of request statuses
- **Interactive Table**: Sortable, filterable requests listing
- **Quick Actions**: Create new request button

### Request Management

- **Create Request Modal**: Form with title, description, category, and priority selection
- **Requests Table**: 
  - Pagination (20 items per page)
  - Search by reference number or title
  - Status filtering
  - Category filtering
  - Priority filtering
- **Status Badges**: Color-coded status indicators
- **Priority Indicators**: Visual priority levels (LOW, MEDIUM, HIGH, CRITICAL)

### UI/UX Features

- **Responsive Design**: Mobile-first approach with breakpoints
- **Loading States**: Skeleton loaders and loading indicators
- **Error Handling**: Toast notifications for errors and success messages
- **Form Validation**: Real-time validation with helpful error messages
- **Accessible Components**: ARIA labels and keyboard navigation support

## Authentication Flow

### Login Process

1. User enters email and password on login page
2. Form validated with Zod schema (email format, password required)
3. API call to `POST /api/v1/auth/login`
4. On success: tokens stored in localStorage + cookie sync
5. User redirected to role-based dashboard

### Token Refresh Mechanism

1. API client attaches access token to all requests
2. On 401 response, automatic refresh attempt
3. Call to `POST /api/v1/auth/refresh` with refresh token
4. New tokens stored, original request retried
5. On refresh failure: redirect to login

### Route Protection

1. Middleware checks for access_token cookie
2. Authenticated users on auth pages redirected to dashboard
3. Unauthenticated users redirected to login
4. Client-side auth context provides user state

## API Integration

### Centralized API Client

The `apiCall` utility in `lib/api.ts` provides:

- Automatic JWT token attachment
- Cookie synchronization for middleware
- Automatic token refresh on 401
- Consistent error handling
- Type-safe response handling

### API Endpoints Consumed

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/v1/auth/login | User login |
| POST | /api/v1/auth/register | User registration |
| POST | /api/v1/auth/refresh | Token refresh |
| GET | /api/v1/auth/me | Get current user |
| GET | /api/v1/requests | List requests (paginated) |
| POST | /api/v1/requests | Create new request |
| GET | /api/v1/categories | List categories |
| GET | /api/v1/locations | List locations |

## Getting Started

### Prerequisites

- Node.js 18+
- pnpm (recommended) or npm/yarn
- Running backend API (default: http://localhost:8080)

### Installation

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install
# or
pnpm install
```

### Configuration

Create a `.env.local` file in the frontend root:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
```

### Development

```bash
# Start development server
npm run dev
# or
pnpm dev
```

The application will be available at http://localhost:3000

### Production Build

```bash
# Create production build
npm run build

# Start production server
npm start
```

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `NEXT_PUBLIC_API_URL` | Yes | http://localhost:8080/api/v1 | Backend API base URL |

## Available Scripts

| Script | Description |
|--------|-------------|
| `npm run dev` | Start development server |
| `npm run build` | Create production build |
| `npm run start` | Start production server |
| `npm run lint` | Run ESLint for code quality |
| `npm run type-check` | Run TypeScript type checking |
| `npm run test` | Run tests with Vitest |

## Component Library

The frontend uses a custom component library built on Radix UI primitives with Tailwind CSS styling.

### Dashboard Components

| Component | Description |
|-----------|-------------|
| `CreateTicketModal` | Modal dialog for creating new service requests |
| `RequestsTable` | Paginated table with search and filtering |
| `Sidebar` | Navigation sidebar with menu items |

### UI Components

| Component | Description |
|-----------|-------------|
| `Button` | Versatile button with variants (default, outline, ghost) |
| `Card` | Content container with header, content, footer |
| `Dialog` | Modal dialog with overlay |
| `Field` | Form field wrapper with label and error handling |
| `Input` | Text input field |
| `InputGroup` | Input with addon elements |
| `Label` | Form field label |
| `Separator` | Visual divider |
| `Textarea` | Multi-line text input |

## Pages

### Public Routes

| Path | Description |
|------|-------------|
| `/` | Root redirect (checks auth, redirects to login or dashboard) |
| `/login` | User login page with email/password form |
| `/register` | User registration with location selection |

### Protected Routes

| Path | Description |
|------|-------------|
| `/dashboard` | Main dashboard with metrics and requests table |
| `/dashboard/requests` | Dedicated requests listing page |
| `/request-form` | Standalone request submission form |

## Contributing

When contributing to the frontend:

1. Follow the existing component patterns in `/components/ui/`
2. Use TypeScript for all new files
3. Run `npm run lint` and `npm run type-check` before committing
4. Use the centralized `apiCall` utility for API requests
5. Maintain consistency with existing naming conventions

## Docker

The frontend can be containerized using Docker:

```bash
# Build the image
docker build -t servicehub-frontend .

# Run the container
docker run -p 3000:3000 servicehub-frontend
```

The Dockerfile is configured for cross-platform compatibility (amd64/arm64).

## License

Internal use only - ServiceHub Project
