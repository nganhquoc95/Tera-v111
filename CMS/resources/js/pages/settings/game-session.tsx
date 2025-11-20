import { useForm } from '@inertiajs/react';
import { useState } from 'react';

import HeadingSmall from '@/components/heading-small';
import InputError from '@/components/input-error';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import AppLayout from '@/layouts/app-layout';
import SettingsLayout from '@/layouts/settings/layout';
import { edit, forceLogout } from '@/routes/game-session';
import { type BreadcrumbItem } from '@/types';

const breadcrumbs: BreadcrumbItem[] = [
    {
        title: 'Game Session',
        href: edit().url,
    },
];

export default function GameSession({
    isLoggedInGame,
    lastLogin,
    sessionIP,
}: {
    isLoggedInGame: boolean;
    lastLogin: string | null;
    sessionIP: string | null;
}) {
    const {
        data: forceLogoutData,
        setData: setForceLogoutData,
        post: postForceLogout,
        processing: forceLogoutProcessing,
        errors: forceLogoutErrors,
    } = useForm({
        password: '',
    });

    const [showSuccess, setShowSuccess] = useState(false);
    const [showForceLogoutModal, setShowForceLogoutModal] = useState(false);

    const handleForceLogout = (e: React.FormEvent) => {
        e.preventDefault();
        postForceLogout(forceLogout().url, {
            onSuccess: () => {
                setShowForceLogoutModal(false);
                setForceLogoutData('password', '');
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
                        title="Game Session"
                        description="Manage your game server connection and session"
                    />

                    {showSuccess && (
                        <div className="rounded-lg bg-green-50 p-4 text-sm text-green-800 dark:bg-green-900/20 dark:text-green-300">
                            Game session has been terminated. You can now log back into the game.
                        </div>
                    )}

                    <div className="rounded-lg border border-blue-200 bg-blue-50 p-4 dark:border-blue-900/30 dark:bg-blue-900/20">
                        <p className="text-sm text-blue-800 dark:text-blue-300">
                            <strong>Note:</strong> This shows your game server session status, not your CMS login. The status below reflects whether you're currently logged into the MapleStory game server.
                        </p>
                    </div>

                    <div className="rounded-lg border border-neutral-200 bg-white p-6 dark:border-neutral-800 dark:bg-neutral-900">
                        <div className="space-y-4">
                            <div>
                                <h3 className="font-semibold">
                                    Current Status
                                </h3>
                                <p className="mt-1 text-sm text-neutral-600 dark:text-neutral-400">
                                    Monitor your active game session information
                                </p>
                                <div className="mt-4 space-y-3 text-sm">
                                    <div className="flex items-center justify-between rounded-md bg-neutral-50 p-3 dark:bg-neutral-800">
                                        <span className="text-neutral-600 dark:text-neutral-400">Game Status:</span>
                                        <span className={`font-semibold ${isLoggedInGame ? 'text-green-600 dark:text-green-400' : 'text-neutral-600 dark:text-neutral-400'}`}>
                                            {isLoggedInGame ? '✓ Logged In' : 'Logged Out'}
                                        </span>
                                    </div>
                                    {sessionIP && (
                                        <div className="flex items-center justify-between rounded-md bg-neutral-50 p-3 dark:bg-neutral-800">
                                            <span className="text-neutral-600 dark:text-neutral-400">Session IP:</span>
                                            <span className="font-mono text-xs font-medium">{sessionIP}</span>
                                        </div>
                                    )}
                                    {lastLogin && (
                                        <div className="flex items-center justify-between rounded-md bg-neutral-50 p-3 dark:bg-neutral-800">
                                            <span className="text-neutral-600 dark:text-neutral-400">Last Login:</span>
                                            <span className="text-xs">{new Date(lastLogin).toLocaleString()}</span>
                                        </div>
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="rounded-lg border border-neutral-200 bg-white p-6 dark:border-neutral-800 dark:bg-neutral-900">
                        <div className="space-y-4">
                            <div>
                                <h3 className="font-semibold">
                                    Force Logout
                                </h3>
                                <p className="mt-1 text-sm text-neutral-600 dark:text-neutral-400">
                                    If you're stuck or unable to login to the game, force logout will terminate your current session.
                                </p>
                            </div>
                            <Button
                                onClick={() =>
                                    setShowForceLogoutModal(true)
                                }
                                variant="destructive"
                                type="button"
                                disabled={!isLoggedInGame}
                            >
                                {isLoggedInGame ? 'Force Logout from Game' : 'Not Logged In'}
                            </Button>
                        </div>
                    </div>

                    {showForceLogoutModal && (
                        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
                            <div className="w-full max-w-md rounded-lg bg-white p-6 dark:bg-neutral-900">
                                <h2 className="mb-4 text-lg font-semibold">
                                    Force Logout from Game
                                </h2>
                                <div className="mb-4 space-y-3">
                                    <p className="text-sm text-neutral-600 dark:text-neutral-400">
                                        This will immediately terminate your game session on the server. Use this if you're stuck or unable to login to the game.
                                    </p>
                                    <div className="rounded-md bg-blue-50 p-3 text-sm text-blue-800 dark:bg-blue-900/20 dark:text-blue-300">
                                        <p className="font-semibold">What this does:</p>
                                        <ul className="mt-2 list-inside space-y-1">
                                            <li>• Clears your active game session</li>
                                            <li>• Resets the login state on the server</li>
                                            <li>• You can immediately log back in</li>
                                        </ul>
                                    </div>
                                    <p className="text-sm font-semibold text-neutral-700 dark:text-neutral-300">
                                        Enter your password to confirm:
                                    </p>
                                </div>

                                <form onSubmit={handleForceLogout}>
                                    <div className="mb-4 space-y-2">
                                        <Label htmlFor="force-logout-password">
                                            Password
                                        </Label>
                                        <Input
                                            id="force-logout-password"
                                            type="password"
                                            value={forceLogoutData.password}
                                            onChange={(e) =>
                                                setForceLogoutData(
                                                    'password',
                                                    e.target.value,
                                                )
                                            }
                                            placeholder="Enter your password"
                                            disabled={forceLogoutProcessing}
                                        />
                                        <InputError
                                            message={
                                                forceLogoutErrors.password
                                            }
                                        />
                                    </div>

                                    <div className="flex gap-2">
                                        <Button
                                            type="button"
                                            variant="outline"
                                            onClick={() => {
                                                setShowForceLogoutModal(
                                                    false,
                                                );
                                                setForceLogoutData(
                                                    'password',
                                                    '',
                                                );
                                            }}
                                            disabled={forceLogoutProcessing}
                                        >
                                            Cancel
                                        </Button>
                                        <Button
                                            type="submit"
                                            variant="destructive"
                                            disabled={forceLogoutProcessing}
                                        >
                                            {forceLogoutProcessing
                                                ? 'Logging out...'
                                                : 'Force Logout'}
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
