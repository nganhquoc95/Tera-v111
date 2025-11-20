import InputError from '@/components/input-error';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Spinner } from '@/components/ui/spinner';
import AuthLayout from '@/layouts/auth-layout';
import { useForm, Head, Link } from '@inertiajs/react';
import { authenticate } from '@/routes/login';
import { request as passwordRequest } from '@/routes/password';

interface LoginErrors {
    name?: string;
    password?: string;
}

export default function Login() {
    const { data, setData, post, errors, processing } = useForm({
        name: '',
        password: '',
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        post(authenticate.url());
    };

    return (
        <AuthLayout
            title="Log in to your account"
            description="Enter your email and password below to log in"
        >
            <Head title="Log in" />

            <form onSubmit={handleSubmit} className="flex flex-col gap-6">
                <div className="grid gap-6">
                    <div className="grid gap-2">
                        <Label htmlFor="name">User name</Label>
                        <Input
                            id="name"
                            type="text"
                            value={data.name}
                            onChange={(e) => setData('name', e.target.value)}
                            required
                            autoFocus
                            autoComplete="name"
                            placeholder="username"
                            disabled={processing}
                        />
                        {errors.name && <InputError message={errors.name} />}
                    </div>

                    <div className="grid gap-2">
                        <Label htmlFor="password">Password</Label>
                        <Input
                            id="password"
                            type="password"
                            value={data.password}
                            onChange={(e) => setData('password', e.target.value)}
                            required
                            autoComplete="current-password"
                            placeholder="••••••••"
                            disabled={processing}
                        />
                        {errors.password && <InputError message={errors.password} />}
                    </div>

                    <div className="flex items-center justify-between">
                        <Link
                            href={passwordRequest().url}
                            className="text-sm text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300"
                        >
                            Forgot password?
                        </Link>
                    </div>

                    <Button
                        type="submit"
                        className="mt-4 w-full"
                        disabled={processing}
                    >
                        {processing && <Spinner />}
                        Log in
                    </Button>
                </div>
            </form>

            <div className="text-center text-sm">
                <span className="text-gray-600 dark:text-gray-400">
                    Don't have an account?{' '}
                </span>
                <Link
                    href="/register"
                    className="font-semibold text-blue-600 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300"
                >
                    Create one
                </Link>
            </div>
        </AuthLayout>
    );
}
