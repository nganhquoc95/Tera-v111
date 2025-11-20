import { useForm } from '@inertiajs/react';
import { FormEventHandler } from 'react';
import InputError from '@/components/input-error';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import AuthLayout from '@/layouts/auth-layout';
import { Link } from '@inertiajs/react';
import { email } from '@/routes/password';
import { authenticate as login } from '@/routes/login';

export default function ForgotPassword({
    status,
    errors,
}: {
    status?: string;
    errors?: Record<string, string[]>;
}) {
    const { data, setData, post, processing } = useForm({
        name: '',
        email: '',
    });

    const submit: FormEventHandler = (e) => {
        e.preventDefault();
        post(email().url);
    };

    return (
        <AuthLayout title="Forgot Password?" description="Enter your account details to reset your password">
            <div className="space-y-6">
                {status && (
                    <div className="rounded-lg bg-green-50 p-4 text-sm text-green-800 dark:bg-green-900/20 dark:text-green-300">
                        {status}
                    </div>
                )}
                {errors && (
                    <div className="rounded-lg bg-red-50 p-4 text-sm text-red-800 dark:bg-red-900/20 dark:text-red-300">
                        {Object.values(errors).flat().map((error, index) => (
                            <p key={index}>{error}</p>
                        ))}
                    </div>
                )}

                <form onSubmit={submit} className="space-y-4">
                    <div className="space-y-2">
                        <Label htmlFor="name">Account Name</Label>
                        <Input
                            id="name"
                            name="name"
                            value={data.name}
                            onChange={(e) => setData('name', e.target.value)}
                            placeholder="Enter your account name"
                            disabled={processing}
                            required
                        />
                        <InputError message={errors?.name?.[0]} />
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="email">Email Address</Label>
                        <Input
                            id="email"
                            name="email"
                            type="email"
                            value={data.email}
                            onChange={(e) => setData('email', e.target.value)}
                            placeholder="Enter your email address"
                            disabled={processing}
                            required
                        />
                        <InputError message={errors?.email?.[0]} />
                    </div>

                    <Button type="submit" className="w-full" disabled={processing}>
                        {processing ? 'Sending...' : 'Send Reset Link'}
                    </Button>
                </form>

                <div className="text-center text-sm">
                    <span className="text-gray-600 dark:text-gray-400">
                        Remember your password?{' '}
                    </span>
                    <Link
                        href={login().url}
                        className="font-semibold text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300"
                    >
                        Sign in
                    </Link>
                </div>
            </div>
        </AuthLayout>
    );
}