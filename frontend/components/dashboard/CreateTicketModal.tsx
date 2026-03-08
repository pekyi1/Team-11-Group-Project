"use client"

import * as React from "react"
import { useForm } from "react-hook-form"
import { Dialog, DialogTrigger, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Textarea } from "@/components/ui/textarea"
import { Field, FieldLabel } from "@/components/ui/field"
import { toast } from "sonner"

type Category = {
  id: number
  name: string
  isActive: boolean
}

type FormData = {
  title: string
  description: string
  categoryId: string
  priority: string
}

type Props = {
  open?: boolean
  onOpenChange?: (v: boolean) => void
  onSuccess?: () => void
}

export default function CreateTicketModal({ open, onOpenChange, onSuccess }: Props) {
  const [internalOpen, setInternalOpen] = React.useState(false)
  const [categories, setCategories] = React.useState<Category[]>([])
  const [loading, setLoading] = React.useState(false)
  const [fetchingCategories, setFetchingCategories] = React.useState(true)

  const isControlled = typeof open === "boolean"
  const value = isControlled ? open : internalOpen
  const setValue = (v: boolean) => {
    if (isControlled) onOpenChange?.(v)
    else setInternalOpen(v)
  }

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormData>({
    defaultValues: {
      title: "",
      description: "",
      categoryId: "",
      priority: "MEDIUM",
    },
  })

  // Fetch categories on mount
  React.useEffect(() => {
    const fetchCategories = async () => {
      try {
        setFetchingCategories(true)
        const response = await fetch("/api/v1/categories")
        if (!response.ok) throw new Error("Failed to fetch categories")
        const data = await response.json()
        setCategories(data.filter((cat: Category) => cat.isActive))
      } catch (error) {
        console.error("Error fetching categories:", error)
        toast.error("Failed to load categories")
      } finally {
        setFetchingCategories(false)
      }
    }

    if (value) {
      fetchCategories()
    }
  }, [value])

  const onSubmit = async (data: FormData) => {
    try {
      setLoading(true)
      const response = await fetch("/api/v1/requests", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          title: data.title,
          description: data.description,
          categoryId: parseInt(data.categoryId),
          priority: data.priority,
        }),
      })

      if (!response.ok) {
        const errorData = await response.json()
        toast.error(errorData.message || "Failed to create request")
        return
      }

      const result = await response.json()
      toast.success(`Request created: ${result.referenceNumber}`)
      reset()
      setValue(false)
      onSuccess?.()
    } catch (error) {
      console.error("Error creating request:", error)
      toast.error("Failed to create request")
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog open={value} onOpenChange={setValue}>
      <DialogTrigger asChild>
        <Button>Create request</Button>
      </DialogTrigger>

      <DialogContent className="w-full max-w-2xl">
        <DialogHeader>
          <DialogTitle>Create Service Request</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="grid gap-4">
          <Field>
            <FieldLabel htmlFor="title">Title *</FieldLabel>
            <Input
              id="title"
              placeholder="e.g. VPN access issue"
              {...register("title", {
                required: "Title is required",
                minLength: { value: 5, message: "Title must be at least 5 characters" },
                maxLength: { value: 200, message: "Title must not exceed 200 characters" },
              })}
              aria-invalid={!!errors.title}
            />
            {errors.title && <span className="text-xs text-destructive">{errors.title.message}</span>}
          </Field>

          <Field>
            <FieldLabel htmlFor="description">Description *</FieldLabel>
            <Textarea
              id="description"
              rows={6}
              placeholder="Describe the problem in detail"
              {...register("description", {
                required: "Description is required",
                minLength: { value: 10, message: "Description must be at least 10 characters" },
                maxLength: { value: 5000, message: "Description must not exceed 5000 characters" },
              })}
              aria-invalid={!!errors.description}
            />
            {errors.description && <span className="text-xs text-destructive">{errors.description.message}</span>}
          </Field>

          <Field>
            <FieldLabel htmlFor="categoryId">Category *</FieldLabel>
            <select
              id="categoryId"
              {...register("categoryId", { required: "Category is required" })}
              className="h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-base shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 aria-invalid:border-destructive aria-invalid:ring-destructive/20"
              aria-invalid={!!errors.categoryId}
              disabled={fetchingCategories}
            >
              <option value="">Select a category</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.id}>
                  {cat.name}
                </option>
              ))}
            </select>
            {errors.categoryId && <span className="text-xs text-destructive">{errors.categoryId.message}</span>}
          </Field>

          <Field>
            <FieldLabel htmlFor="priority">Priority *</FieldLabel>
            <select
              id="priority"
              {...register("priority", { required: "Priority is required" })}
              className="h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-base shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
              aria-invalid={!!errors.priority}
            >
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
            {errors.priority && <span className="text-xs text-destructive">{errors.priority.message}</span>}
          </Field>
        </form>

        <DialogFooter>
          <Button variant="outline" onClick={() => setValue(false)} disabled={loading}>
            Cancel
          </Button>
          <Button onClick={handleSubmit(onSubmit)} disabled={loading}>
            {loading ? "Creating..." : "Submit"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
