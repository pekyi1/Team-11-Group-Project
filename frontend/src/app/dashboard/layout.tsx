import "@/src/app/globals.css"
import Sidebar from "@/components/dashboard/Sidebar"
import { AuthProvider } from "@/lib/AuthContext"

export const metadata = {
  title: "Dashboard - ServiceHub",
}

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <AuthProvider>
      <div className="min-h-screen w-screen bg-background">
        <div className="mx-auto flex max-w-[1200px] gap-6 px-4 py-8">
          <Sidebar />
          <main className="flex w-full flex-col gap-6">{children}</main>
        </div>
      </div>
    </AuthProvider>
  )
}