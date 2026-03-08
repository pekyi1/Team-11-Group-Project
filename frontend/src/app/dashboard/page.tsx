"use client"

import * as React from "react"
import { useRouter } from "next/navigation"
import { useAuth } from "@/lib/AuthContext"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import CreateTicketModal from "@/components/dashboard/CreateTicketModal"
import RequestsTable from "@/components/dashboard/RequestsTable"
import { AlertCircle, TrendingUp, LogOut } from "lucide-react"
import { toast } from "sonner"
import { apiCall } from "@/lib/api"

type DashboardMetrics = {
  openRequests: number
  assignedRequests: number
  inProgressRequests: number
  resolvedRequests: number
  closedRequests: number
  totalRequests: number
  slaBreachedCount: number
  averageResponseTime: string
  slaCompliancePercentage: number
}

export default function DashboardPage() {
  const router = useRouter()
  const { user, isLoading: authLoading, logout } = useAuth()
  const [metrics, setMetrics] = React.useState<DashboardMetrics | null>(null)
  const [loading, setLoading] = React.useState(true)
  const [refreshTrigger, setRefreshTrigger] = React.useState(0)

// //   Protect dashboard from unauthenticated access
//   React.useEffect(() => {
//     if (!authLoading && !user) {
//       router.push('/login')
//     }
//   }, [authLoading, user, router])

  const fetchMetrics = React.useCallback(async () => {
    try {
      setLoading(true)
      const response = await apiCall("/requests")
      if (!response.ok) throw new Error("Failed to fetch metrics")

      const data = await response.json()
      const content = data.content || []

      const statusCounts = {
        OPEN: content.filter((r: any) => r.status === "OPEN").length,
        ASSIGNED: content.filter((r: any) => r.status === "ASSIGNED").length,
        IN_PROGRESS: content.filter((r: any) => r.status === "IN_PROGRESS").length,
        RESOLVED: content.filter((r: any) => r.status === "RESOLVED").length,
        CLOSED: content.filter((r: any) => r.status === "CLOSED").length,
      }

      const slaBreached = content.filter((r: any) => !r.responseSlaMet || !r.resolutionSlaMet).length
      const total = content.length

      setMetrics({
        openRequests: statusCounts.OPEN,
        assignedRequests: statusCounts.ASSIGNED,
        inProgressRequests: statusCounts.IN_PROGRESS,
        resolvedRequests: statusCounts.RESOLVED,
        closedRequests: statusCounts.CLOSED,
        totalRequests: total,
        slaBreachedCount: slaBreached,
        averageResponseTime: "4h 12m", // TODO: Calculate from actual data
        slaCompliancePercentage: total > 0 ? Math.round(((total - slaBreached) / total) * 100) : 100,
      })
    } catch (error) {
      console.error("Error fetching metrics:", error)
      toast.error("Failed to load metrics")
    } finally {
      setLoading(false)
    }
  }, [])

  React.useEffect(() => {
    fetchMetrics()
  }, [fetchMetrics, refreshTrigger])

  const handleRequestCreated = () => {
    setRefreshTrigger((prev) => prev + 1)
    window.dispatchEvent(new Event("refresh-requests"))
  }

  if (loading && !metrics) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold mb-4">Overview</h1>
          </div>
        </div>
        <p className="text-muted-foreground">Loading dashboard...</p>
      </div>
    )
  }

  return (
    <>
      <div className="flex items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-semibold mb-2">Dashboard</h1>
          <p className="text-sm text-muted-foreground">Summary of your service requests and SLA status</p>
        </div>

        <div className="flex items-center gap-4">
          {user && (
            <div className="flex items-center gap-3">
              <div className="text-right hidden sm:block">
                <p className="text-sm font-medium">{user.fullName}</p>
                <p className="text-xs text-muted-foreground">{user.email}</p>
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={logout}
                className="gap-2"
              >
                <LogOut className="h-4 w-4" />
                <span className="hidden sm:inline">Logout</span>
              </Button>
            </div>
          )}
          <CreateTicketModal onSuccess={handleRequestCreated} />
        </div>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-4 mb-6">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Total Requests</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{metrics?.totalRequests || 0}</div>
            <p className="text-xs text-muted-foreground mt-1">All-time requests</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Open</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-blue-600">{metrics?.openRequests || 0}</div>
            <p className="text-xs text-muted-foreground mt-1">Awaiting action</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">In Progress</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-yellow-600">{metrics?.inProgressRequests || 0}</div>
            <p className="text-xs text-muted-foreground mt-1">Being worked on</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
              <AlertCircle className="h-4 w-4 text-red-600" />
              SLA Breached
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-red-600">{metrics?.slaBreachedCount || 0}</div>
            <p className="text-xs text-muted-foreground mt-1">Overdue SLA</p>
          </CardContent>
        </Card>
      </div>

      {/* Secondary Metrics */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2 mb-6">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">SLA Compliance</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-baseline gap-2">
              <div className="text-3xl font-bold">{metrics?.slaCompliancePercentage || 100}%</div>
              <TrendingUp className="h-4 w-4 text-green-600" />
            </div>
            <div className="mt-2 h-2 w-full bg-muted rounded-full overflow-hidden">
              <div
                className="h-full bg-green-600"
                style={{ width: `${metrics?.slaCompliancePercentage || 100}%` }}
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Request Status Breakdown</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span>Assigned:</span>
                <span className="font-medium">{metrics?.assignedRequests || 0}</span>
              </div>
              <div className="flex justify-between">
                <span>Resolved:</span>
                <span className="font-medium">{metrics?.resolvedRequests || 0}</span>
              </div>
              <div className="flex justify-between">
                <span>Closed:</span>
                <span className="font-medium">{metrics?.closedRequests || 0}</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Requests Table */}
      <RequestsTable key={refreshTrigger} />
    </>
  )
}

