'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { getAccessToken, getRoleDashboardPath, getStoredUser } from '@/lib/auth';

export default function HomePage() {
  const router = useRouter();

  useEffect(() => {
    const token = getAccessToken();
    const user = getStoredUser();

    if (token && user) {
      // Redirect authenticated user to their role-based dashboard
      const dashboardPath = getRoleDashboardPath(user.role);
      router.push(dashboardPath);
    } else {
      // Redirect unauthenticated user to login
      router.push('/login');
    }
  }, [router]);

  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="text-center">
        <p>Loading...</p>
      </div>
    </div>
  );
}


