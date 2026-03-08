import { NextRequest, NextResponse } from 'next/server';

export async function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  const token = await request.cookies.get('access_token')?.value

  const isAuthPage = pathname.startsWith('/login') || pathname.startsWith('/register');
  const isPublicPage = pathname === '/' || pathname.startsWith('/auth');

  if (token && isAuthPage) {
    return NextResponse.redirect(new URL('/dashboard', request.url));
  }

  if (!token && !isAuthPage && !isPublicPage) {
    return NextResponse.redirect(new URL('/login', request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    // Skip api routes, static files, etc
    '/((?!api|_next/static|_next/image|favicon.ico).*)',
  ],
};

