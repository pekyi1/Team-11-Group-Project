'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Field,
  FieldLabel,
  FieldError,
  FieldDescription,
} from '@/components/ui/field';
import { apiCall } from '@/lib/api';
import '@/src/app/globals.css';

interface Location {
  id: string;
  name: string;
}

const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[a-zA-Z\d@$!%*?&]{8,}$/;

const registerSchema = z
  .object({
    fullName: z.string().min(1, 'Full name is required'),
    email: z.string().email('Invalid email format'),
    password: z
      .string()
      .regex(
        passwordRegex,
        'Password must be at least 8 characters with uppercase, lowercase, number, and special character'
      ),
    passwordConfirm: z.string().min(1, 'Password confirmation is required'),
    locationId: z.string().min(1, 'Location is required'),
  })
  .refine((data) => data.password === data.passwordConfirm, {
    message: 'Passwords do not match',
    path: ['passwordConfirm'],
  });

type RegisterFormData = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(false);
  const [locations, setLocations] = useState<Location[]>([]);
  const [locationsLoading, setLocationsLoading] = useState(true);

  const {
    register,
    handleSubmit,
    formState: { errors },
    setError,
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    mode: 'onBlur',
  });

  // Fetch locations on mount
  useEffect(() => {
    const fetchLocations = async () => {
      try {
        const response = await apiCall('/locations', { skipAuth: true });
        if (response.ok) {
          const data = await response.json();
          setLocations(data.content || data);
        }
      } catch (error) {
        console.error('Failed to fetch locations:', error);
        toast.error('Failed to load locations');
      } finally {
        setLocationsLoading(false);
      }
    };

    fetchLocations();
  }, []);

  const onSubmit = async (data: RegisterFormData) => {
    setIsLoading(true);
    try {
      const response = await apiCall('/auth/register', {
        method: 'POST',
        body: JSON.stringify({
          fullName: data.fullName,
          email: data.email,
          password: data.password,
          locationId: data.locationId,
        }),
        skipAuth: true,
      });

      if (!response.ok) {
        const error = await response.json();
        if (error.message?.includes('email')) {
          setError('email', { message: 'An account with this email already exists.' });
        } else {
          toast.error(error.message || 'Registration failed');
        }
        return;
      }

      toast.success('Registration successful! Redirecting to login...');
      setTimeout(() => router.push('/login'), 1500);
    } catch (error) {
      console.error('Registration error:', error);
      toast.error('An error occurred during registration');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50 py-8">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Register for ServiceHub</CardTitle>
          <CardDescription>
            Create an account to start submitting service requests
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <Field>
              <FieldLabel htmlFor="fullName">Full Name</FieldLabel>
              <Input
                id="fullName"
                type="text"
                placeholder="John Doe"
                {...register('fullName')}
                disabled={isLoading}
              />
              {errors.fullName && <FieldError>{errors.fullName.message}</FieldError>}
            </Field>

            <Field>
              <FieldLabel htmlFor="email">Email</FieldLabel>
              <Input
                id="email"
                type="email"
                placeholder="you@example.com"
                {...register('email')}
                disabled={isLoading}
              />
              {errors.email && <FieldError>{errors.email.message}</FieldError>}
            </Field>

            <Field>
              <FieldLabel htmlFor="locationId">Location</FieldLabel>
              {locationsLoading ? (
                <Input disabled value="Loading locations..." />
              ) : (
                <select
                  id="locationId"
                  {...register('locationId')}
                  disabled={isLoading || locationsLoading}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md"
                >
                  <option value="">Select a location</option>
                  {locations.map((loc) => (
                    <option key={loc.id} value={loc.id}>
                      {loc.name}
                    </option>
                  ))}
                </select>
              )}
              {errors.locationId && <FieldError>{errors.locationId.message}</FieldError>}
            </Field>

            <Field>
              <FieldLabel htmlFor="password">Password</FieldLabel>
              <Input
                id="password"
                type="password"
                placeholder="Min 8 chars, uppercase, lowercase, number, special char"
                {...register('password')}
                disabled={isLoading}
              />
              {errors.password && <FieldError>{errors.password.message}</FieldError>}
              <FieldDescription>
                Must contain uppercase, lowercase, number, and special character
              </FieldDescription>
            </Field>

            <Field>
              <FieldLabel htmlFor="passwordConfirm">Confirm Password</FieldLabel>
              <Input
                id="passwordConfirm"
                type="password"
                placeholder="Confirm your password"
                {...register('passwordConfirm')}
                disabled={isLoading}
              />
              {errors.passwordConfirm && (
                <FieldError>{errors.passwordConfirm.message}</FieldError>
              )}
            </Field>

            <Button
              type="submit"
              className="w-full"
              disabled={isLoading || locationsLoading}
            >
              {isLoading ? 'Registering...' : 'Register'}
            </Button>

            <p className="text-sm text-center text-gray-600">
              Already have an account?{' '}
              <a href="/login" className="text-blue-600 hover:underline">
                Login here
              </a>
            </p>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

