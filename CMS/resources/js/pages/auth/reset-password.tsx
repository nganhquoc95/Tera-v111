import { useForm } from '@inertiajs/react';
import { FormEventHandler } from 'react';
import InputError from '@/components/input-error';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import AuthLayout from '@/layouts/auth-layout';
import { update as resetPassword } from '@/routes/password/reset';

interface ResetPasswordProps {
    key: string;
    email: string;
}

export default function ResetPassword({ key, email }: ResetPasswordProps) {
    const { data, setData, post, processing, errors } = useForm({
        key: key,
        password: '',
        password_confirmation: '',
    });

    const submit: FormEventHandler = (e) => {
        e.preventDefault();
        post(resetPassword().url);
    };

    return (
        <AuthLayout
            title="Reset Password"
            description="Please enter your new password below"
        >
            <form onSubmit={submit} className="space-y-6">
                <div className="space-y-2">
                    <Label htmlFor="email">Email</Label>
                    <Input
                        id="email"
                        type="email"
                        value={email}
                        className="mt-1 block w-full"
                        readOnly
                    />
                </div>

                <div className="space-y-2">
                    <Label htmlFor="password">New Password</Label>
                    <Input
                        id="password"
                        type="password"
                        name="password"
                        value={data.password}
                        onChange={(e) => setData('password', e.target.value)}
                        placeholder="Enter new password"
                        disabled={processing}
                        required
                    />
                    <InputError message={errors.password} />
                </div>

                <div className="space-y-2">
                    <Label htmlFor="password_confirmation">Confirm Password</Label>
                    <Input
                        id="password_confirmation"
                        type="password"
                        name="password_confirmation"
                        value={data.password_confirmation}
                        onChange={(e) => setData('password_confirmation', e.target.value)}
                        placeholder="Confirm new password"
                        disabled={processing}
                        required
                    />
                    <InputError message={errors.password_confirmation} />
                </div>

                <Button type="submit" className="w-full" disabled={processing}>
                    {processing ? 'Resetting...' : 'Reset Password'}
                </Button>
            </form>
        </AuthLayout>
    );
}
