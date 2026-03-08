"use client"

import * as React from "react"
import { createPortal } from "react-dom"
import { cn } from "@/lib/utils"

type DialogContextType = {
  open: boolean
  onOpenChange: (v: boolean) => void
}

const DialogContext = React.createContext<DialogContextType | null>(null)

function Dialog({ children, open, onOpenChange }: any) {
  return (
    <DialogContext.Provider value={{ open: !!open, onOpenChange: onOpenChange ?? (() => {}) }}>
      {children}
    </DialogContext.Provider>
  )
}

function DialogTrigger({ children }: any) {
  const ctx = React.useContext(DialogContext)

  if (!ctx) return children

  // Expect a single element as child
  const child = React.Children.only(children) as React.ReactElement
  return React.cloneElement(child, {
    onClick: (e: any) => {
      ctx.onOpenChange(!ctx.open)
      child.props?.onClick?.(e)
    },
  })
}

function DialogContent({ className, children }: any) {
  const ctx = React.useContext(DialogContext)
  const isClient = typeof window !== "undefined"

  if (!ctx || !isClient || !ctx.open) return null

  return createPortal(
    <div className="fixed inset-0 z-[60] flex items-center justify-center">
      <div
        className="absolute inset-0 bg-black/40"
        onClick={() => ctx.onOpenChange(false)}
      />

      <div
        role="dialog"
        aria-modal="true"
        className={cn(
          "relative z-[70] w-[90vw] max-w-2xl rounded-xl bg-card p-6 shadow-lg",
          className
        )}
      >
        {children}
      </div>
    </div>,
    document.body
  )
}

function DialogHeader({ className, ...props }: any) {
  return <div className={cn("flex flex-col gap-1.5", className)} {...props} />
}

function DialogFooter({ className, ...props }: any) {
  return <div className={cn("flex items-center justify-end gap-2", className)} {...props} />
}

function DialogTitle({ className, ...props }: any) {
  return <h3 className={cn("text-lg font-semibold", className)} {...props} />
}

export { Dialog, DialogTrigger, DialogContent, DialogHeader, DialogFooter, DialogTitle }
