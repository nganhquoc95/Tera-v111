import { useForm } from '@inertiajs/react';
import { useState } from 'react';

import HeadingSmall from '@/components/heading-small';
import InputError from '@/components/input-error';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import AppLayout from '@/layouts/app-layout';
import SettingsLayout from '@/layouts/settings/layout';
import { edit, reset2ndPassword, update } from '@/routes/security';
import { type BreadcrumbItem } from '@/types';

const breadcrumbs: BreadcrumbItem[] = [
    {
        title: 'Security settings',
        href: edit().url,
    },
];

export default function Security({
    picEnabled,
    has2ndPassword,
}: {
    picEnabled: boolean;
    has2ndPassword: boolean;
}) {
    const { data, setData, patch, processing, errors } = useForm({
        pic_enabled: picEnabled,
    });

    const {
        data: reset2ndData,
        setData: setReset2ndData,
        post: postReset2nd,
        processing: reset2ndProcessing,
        errors: reset2ndErrors,
    } = useForm({
        password: '',
    });

    const [showSuccess, setShowSuccess] = useState(false);
    const [show2ndPasswordModal, setShow2ndPasswordModal] = useState(false);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        patch(update().url, {
            onSuccess: () => {
                setShowSuccess(true);
                setTimeout(() => setShowSuccess(false), 3000);
            },
        });
    };

    const handleReset2ndPassword = (e: React.FormEvent) => {
        e.preventDefault();
        postReset2nd(reset2ndPassword().url, {
            onSuccess: () => {
                setShow2ndPasswordModal(false);
                setReset2ndData('password', '');
                setShowSuccess(true);
                setTimeout(() => setShowSuccess(false), 3000);
            },
        });
    };

    return (
        <AppLayout breadcrumbs={breadcrumbs}>
            <SettingsLayout>
                <div className="space-y-6">
                    <HeadingSmall
                        title="Security Settings"
                        description="Manage your account security options"
                    />

                    <form onSubmit={handleSubmit} className="space-y-6">
                        <div className="rounded-lg border border-neutral-200 bg-white p-6 dark:border-neutral-800 dark:bg-neutral-900">
                            <div className="flex items-center space-x-3">
                                <Checkbox
                                    id="pic_enabled"
                                    checked={data.pic_enabled}
                                    onCheckedChange={(checked) =>
                                        setData('pic_enabled', !!checked)
                                    }
                                    disabled={processing}
                                />
                                <div className="flex-1">
                                    <Label
                                        htmlFor="pic_enabled"
                                        className="cursor-pointer font-medium"
                                    >
                                        Enable Picture (PIC) Password
                                    </Label>
                                    <p className="mt-1 text-sm text-neutral-600 dark:text-neutral-400">
                                        When enabled, you'll use a picture
                                        password along with your regular
                                        password for enhanced security.
                                    </p>
                                </div>
                            </div>
                        </div>

                        {errors.pic_enabled && (
                            <p className="text-sm text-red-600">
                                {errors.pic_enabled}
                            </p>
                        )}

                        <Button disabled={processing} type="submit">
                            {processing ? 'Saving...' : 'Save Changes'}
                        </Button>

                        {showSuccess && (
                            <div className="text-sm text-green-600">
                                Security settings updated successfully.
                            </div>
                        )}
                    </form>

                    {has2ndPassword && (
                        <div className="rounded-lg border border-neutral-200 bg-white p-6 dark:border-neutral-800 dark:bg-neutral-900">
                            <div className="space-y-4">
                                <div>
                                    <h3 className="font-semibold">
                                        2nd Password (PIC)
                                    </h3>
                                    <p className="mt-1 text-sm text-neutral-600 dark:text-neutral-400">
                                        Reset your 2nd password and clear the
                                        associated security data.
                                    </p>
                                </div>
                                <Button
                                    onClick={() =>
                                        setShow2ndPasswordModal(true)
                                    }
                                    variant="destructive"
                                    type="button"
                                >
                                    Reset 2nd Password
                                </Button>
                            </div>
                        </div>
                    )}

                    {show2ndPasswordModal && (
                        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
                            <div className="w-full max-w-md rounded-lg bg-white p-6 dark:bg-neutral-900">
                                <h2 className="mb-4 text-lg font-semibold">
                                    Reset 2nd Password
                                </h2>
                                <p className="mb-4 text-sm text-neutral-600 dark:text-neutral-400">
                                    Enter your password to confirm resetting
                                    your 2nd password.
                                </p>

                                <form onSubmit={handleReset2ndPassword}>
                                    <div className="mb-4 space-y-2">
                                        <Label htmlFor="confirm-password">
                                            Password
                                        </Label>
                                        <Input
                                            id="confirm-password"
                                            type="password"
                                            value={reset2ndData.password}
                                            onChange={(e) =>
                                                setReset2ndData(
                                                    'password',
                                                    e.target.value,
                                                )
                                            }
                                            placeholder="Enter your password"
                                            disabled={reset2ndProcessing}
                                        />
                                        <InputError
                                            message={
                                                reset2ndErrors.password
                                            }
                                        />
                                    </div>

                                    <div className="flex gap-2">
                                        <Button
                                            type="button"
                                            variant="outline"
                                            onClick={() => {
                                                setShow2ndPasswordModal(
                                                    false,
                                                );
                                                setReset2ndData(
                                                    'password',
                                                    '',
                                                );
                                            }}
                                            disabled={reset2ndProcessing}
                                        >
                                            Cancel
                                        </Button>
                                        <Button
                                            type="submit"
                                            variant="destructive"
                                            disabled={reset2ndProcessing}
                                        >
                                            {reset2ndProcessing
                                                ? 'Resetting...'
                                                : 'Reset'}
                                        </Button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    )}
                </div>
            </SettingsLayout>
        </AppLayout>
    );
}
