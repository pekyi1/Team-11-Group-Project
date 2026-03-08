"use client"

import * as React from "react"
import Link from "next/link"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { menu } from "@/data/menu"

export default function Sidebar({ className }: { className?: string }) {

  return (
    <aside
      className={cn(
        "w-72 shrink-0 border-r bg-sidebar p-4 dark:bg-[#0b1020]",
        "hidden lg:flex lg:flex-col gap-4",
        className
      )}
    >
      <div className="flex items-center gap-3 px-2">
        <div className="h-10 w-10 rounded-md bg-primary" />
        <div>
          <h3 className="text-lg font-semibold">ServiceHub</h3>
          <p className="text-xs text-muted-foreground">Ticketing</p>
        </div>
      </div>

      <Separator />

      <nav className="flex grow flex-col gap-1 px-2">
        {menu.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={cn(
              "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium text-muted-foreground hover:bg-primary/10 hover:text-primary",
              "transition-colors"
            )}
          >
            <span className="h-4 w-4 rounded bg-primary/70" />
            {item.label}
          </Link>
        ))}
      </nav>

      <div className="mt-auto px-2">
        <Button variant="outline" className="w-full">
          Log out
        </Button>
      </div>
    </aside>
  )
}

