import { useForm } from '@inertiajs/react';
import { FormEventHandler } from 'react';
import InputError from '@/components/input-error';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import AuthLayout from '@/layouts/auth-layout';
import { Link } from '@inertiajs/react';
import { create } from '@/routes/register';

export default function Register() {
    const { data, setData, post, processing, errors } = useForm({
        name: '',
        email: '',
        password: '',
        password_confirmation: '',
    });

    const submit: FormEventHandler = (e) => {
        e.preventDefault();
        post(create().url);
    };

    return (
        <AuthLayout
            title="Create an account"
            description="Enter your details below to create your account"
        >
            <form onSubmit={submit} className="space-y-6">
                <div className="space-y-4">
                    <div className="space-y-2">
                        <Label htmlFor="name">Account Name</Label>
                        <Input
                            id="name"
                            type="text"
                            value={data.name}
                            onChange={(e) => setData('name', e.target.value)}
                            placeholder="4-14 characters"
                            disabled={processing}
                            required
                            autoFocus
                        />
                        <InputError message={errors.name} />
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="email">Email Address</Label>
                        <Input
                            id="email"
                            type="email"
                            value={data.email}
                            onChange={(e) => setData('email', e.target.value)}
                            placeholder="email@example.com"
                            disabled={processing}
                            required
                        />
                        <InputError message={errors.email} />
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="password">Password</Label>
                        <Input
                            id="password"
                            type="password"
                            value={data.password}
                            onChange={(e) => setData('password', e.target.value)}
                            placeholder="Min 8 chars, uppercase, numbers, symbols"
                            disabled={processing}
                            required
                        />
                        <InputError message={errors.password} />
                        <p className="text-xs text-muted-foreground">
                            Must contain at least 8 characters, 1 uppercase letter, 1 number, and 1 symbol
                        </p>
                    </div>

                    <div className="space-y-2">
                        <Label htmlFor="password_confirmation">Confirm Password</Label>
                        <Input
                            id="password_confirmation"
                            type="password"
                            value={data.password_confirmation}
                            onChange={(e) => setData('password_confirmation', e.target.value)}
                            placeholder="Confirm your password"
                            disabled={processing}
                            required
                        />
                        <InputError message={errors.password_confirmation} />
                    </div>

                    <Button type="submit" className="w-full" disabled={processing}>
                        {processing ? 'Creating account...' : 'Create Account'}
                    </Button>
                </div>
            </form>

            <div className="text-center text-sm">
                <span className="text-gray-600 dark:text-gray-400">
                    Already have an account?{' '}
                </span>
                <Link
                    href="/login"
                    className="font-semibold text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300"
                >
                    Sign in
                </Link>
            </div>
        </AuthLayout>
    );
}
