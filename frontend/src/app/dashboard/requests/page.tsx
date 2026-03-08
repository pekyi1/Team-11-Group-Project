"use client"

import * as React from "react"
import RequestsTable from "@/components/dashboard/RequestsTable"

export default function RequestsPage() {
  return (
    <div>
      <h1 className="text-2xl font-semibold mb-4">Requests</h1>
      <p className="text-sm text-muted-foreground mb-8">List of recent service requests</p>

      <RequestsTable />
    </div>
  )
}

