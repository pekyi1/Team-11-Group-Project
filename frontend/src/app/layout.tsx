import { AuthProvider } from '@/lib/AuthContext';

export const metadata = {
  title: "ServiceHub",
  description: "Internal Service Request & Ticketing System",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
