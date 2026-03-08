"use client"

import * as React from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { ChevronLeft, ChevronRight, Search } from "lucide-react"
import { Input } from "@/components/ui/input"
import { toast } from "sonner"
import { apiCall } from '@/lib/api';

type Request = {
  id: string
  referenceNumber: string
  title: string
  status: string
  priority: string
  categoryName: string
  requesterName: string
  createdAt: string
}

type PageData = {
  content: Request[]
  totalElements: number
  totalPages: number
  currentPage: number
  pageSize: number
}

const STATUS_COLORS: Record<string, string> = {
  OPEN: "bg-blue-100 text-blue-800",
  ASSIGNED: "bg-purple-100 text-purple-800",
  IN_PROGRESS: "bg-yellow-100 text-yellow-800",
  RESOLVED: "bg-green-100 text-green-800",
  CLOSED: "bg-gray-100 text-gray-800",
}

const PRIORITY_COLORS: Record<string, string> = {
  LOW: "text-green-600",
  MEDIUM: "text-yellow-600",
  HIGH: "text-orange-600",
  CRITICAL: "text-red-600",
}

interface RequestsTableProps {
  className?: string
  onRefresh?: () => void
}

export default function RequestsTable({ className, onRefresh }: RequestsTableProps) {
  const [requests, setRequests] = React.useState<Request[]>([])
  const [pageData, setPageData] = React.useState<PageData>({
    content: [],
    totalElements: 0,
    totalPages: 0,
    currentPage: 0,
    pageSize: 20,
  })
  const [loading, setLoading] = React.useState(true)
  const [currentPage, setCurrentPage] = React.useState(0)
  const [searchTerm, setSearchTerm] = React.useState("")
  const [statusFilter, setStatusFilter] = React.useState<string>("")
  const [categoryFilter, setCategoryFilter] = React.useState<string>("")
  const [priorityFilter, setPriorityFilter] = React.useState<string>("")

  // Fetch requests
  const fetchRequests = React.useCallback(async () => {
    try {
      setLoading(true)
      const params = new URLSearchParams({
        page: currentPage.toString(),
        size: "20",
        sort: "createdAt,desc",
      })

      if (statusFilter) params.append("status", statusFilter)
      if (categoryFilter) params.append("categoryId", categoryFilter)
      if (priorityFilter) params.append("priority", priorityFilter)

      const response = await apiCall(`/requests?${params.toString()}`)
      if (!response.ok) {
        throw new Error("Failed to fetch requests")
      }

      const data = await response.json()
      setPageData({
        content: data.content || [],
        totalElements: data.totalElements || 0,
        totalPages: data.totalPages || 0,
        currentPage: data.number || 0,
        pageSize: data.size || 20,
      })
      setRequests(data.content || [])
    } catch (error) {
      console.error("Error fetching requests:", error)
      toast.error("Failed to load requests")
    } finally {
      setLoading(false)
    }
  }, [currentPage, statusFilter, categoryFilter, priorityFilter])

  React.useEffect(() => {
    fetchRequests()
  }, [fetchRequests])

  // Handle refresh from parent
  React.useEffect(() => {
    const handleRefresh = () => {
      setCurrentPage(0)
      fetchRequests()
    }

    window.addEventListener("refresh-requests", handleRefresh)
    return () => window.removeEventListener("refresh-requests", handleRefresh)
  }, [fetchRequests])

  const handleSearch = (value: string) => {
    setSearchTerm(value)
    setCurrentPage(0)
  }

  const handleStatusFilter = (status: string) => {
    setStatusFilter(status === statusFilter ? "" : status)
    setCurrentPage(0)
  }

  const handlePreviousPage = () => {
    setCurrentPage(Math.max(0, currentPage - 1))
  }

  const handleNextPage = () => {
    if (currentPage < pageData.totalPages - 1) {
      setCurrentPage(currentPage + 1)
    }
  }

  const filteredRequests = requests.filter(
    (r) =>
      r.referenceNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
      r.title.toLowerCase().includes(searchTerm.toLowerCase())
  )

  if (loading && requests.length === 0) {
    return (
      <Card className={cn("w-full overflow-x-auto", className)}>
        <CardHeader>
          <CardTitle>Service Requests</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center justify-center py-12">
            <p className="text-muted-foreground">Loading requests...</p>
          </div>
        </CardContent>
      </Card>
    )
  }

  if (filteredRequests.length === 0 && searchTerm === "" && !statusFilter && !categoryFilter && !priorityFilter) {
    return (
      <Card className={cn("w-full overflow-x-auto", className)}>
        <CardHeader>
          <CardTitle>Service Requests</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col items-center justify-center py-12">
            <p className="text-muted-foreground mb-2">No service requests yet</p>
            <p className="text-sm text-muted-foreground">Create your first request to get started</p>
          </div>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className={cn("w-full overflow-x-auto", className)}>
      <CardHeader>
        <CardTitle>Service Requests</CardTitle>
      </CardHeader>
      <CardContent>
        {/* Search and Filters */}
        <div className="mb-6 flex flex-col gap-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search by reference number or title..."
              value={searchTerm}
              onChange={(e) => handleSearch(e.target.value)}
              className="pl-10"
            />
          </div>

          <div className="flex flex-wrap gap-2">
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium text-muted-foreground">Status:</span>
              {["OPEN", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED"].map((status) => (
                <Button
                  key={status}
                  variant={statusFilter === status ? "default" : "outline"}
                  size="sm"
                  onClick={() => handleStatusFilter(status)}
                >
                  {status.replace(/_/g, " ")}
                </Button>
              ))}
            </div>
          </div>
        </div>

        {/* Table */}
        <div className="overflow-x-auto">
          <table className="w-full min-w-[720px] table-auto">
            <thead className="text-left text-sm text-muted-foreground font-medium">
              <tr>
                <th className="py-3">ID</th>
                <th className="py-3">Title</th>
                <th className="py-3">Category</th>
                <th className="py-3">Status</th>
                <th className="py-3">Priority</th>
                <th className="py-3">Created</th>
                <th className="py-3">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {filteredRequests.map((r) => (
                <tr key={r.id} className="align-top hover:bg-muted/50">
                  <td className="py-3 font-medium text-sm">{r.referenceNumber}</td>
                  <td className="py-3 text-sm max-w-xs truncate">{r.title}</td>
                  <td className="py-3 text-sm">{r.categoryName}</td>
                  <td className="py-3">
                    <span
                      className={cn(
                        "inline-flex items-center gap-2 rounded-md px-2 py-1 text-xs font-medium",
                        STATUS_COLORS[r.status] || "bg-gray-100 text-gray-800"
                      )}
                    >
                      {r.status.replace(/_/g, " ")}
                    </span>
                  </td>
                  <td className="py-3 text-sm">
                    <span className={cn("font-medium", PRIORITY_COLORS[r.priority])}>{r.priority}</span>
                  </td>
                  <td className="py-3 text-sm">{new Date(r.createdAt).toLocaleDateString()}</td>
                  <td className="py-3 text-sm">
                    <Button variant="ghost" size="sm">
                      View
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {pageData.totalPages > 1 && (
          <div className="mt-6 flex items-center justify-between border-t pt-4">
            <div className="text-sm text-muted-foreground">
              Page {pageData.currentPage + 1} of {pageData.totalPages} • {pageData.totalElements} total requests
            </div>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={handlePreviousPage}
                disabled={currentPage === 0}
              >
                <ChevronLeft className="h-4 w-4" />
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={handleNextPage}
                disabled={currentPage >= pageData.totalPages - 1}
              >
                Next
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  )
}


