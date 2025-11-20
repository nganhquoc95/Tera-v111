import {
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
} from '@/components/ui/dropdown-menu';
import { UserInfo } from '@/components/user-info';
import { useMobileNavigation } from '@/hooks/use-mobile-navigation';
import { logout } from '@/routes';
import { edit } from '@/routes/profile';
import { type User } from '@/types';
import { Link, router } from '@inertiajs/react';
import { LogOut, Settings, Coins, Gift, Zap } from 'lucide-react';

interface UserMenuContentProps {
    user: User;
}

export function UserMenuContent({ user }: UserMenuContentProps) {
    const cleanup = useMobileNavigation();

    const handleLogout = () => {
        cleanup();
        router.flushAll();
    };

    const nxCredit = (user as any).NxCredit || 0;
    const mPoints = (user as any).mPoints || 0;
    const nxPrepaid = (user as any).NxPrepaid || 0;

    return (
        <>
            <DropdownMenuLabel className="p-0 font-normal">
                <div className="flex items-center gap-2 px-1 py-1.5 text-left text-sm">
                    <UserInfo user={user} showEmail={true} />
                </div>
                <hr />
                <div className="mt-2 space-y-1 text-xs text-muted-foreground" style={{ paddingLeft: 42 }}>
                    <div className="flex items-center gap-1">
                        <Coins className="h-3.5 w-3.5" />
                        <span>NxCredit: <strong>{nxCredit.toLocaleString()}</strong></span>
                    </div>
                    <div className="flex items-center gap-1">
                        <Gift className="h-3.5 w-3.5" />
                        <span>mPoints: <strong>{mPoints.toLocaleString()}</strong></span>
                    </div>
                    <div className="flex items-center gap-1">
                        <Zap className="h-3.5 w-3.5" />
                        <span>NxPrepaid: <strong>{nxPrepaid.toLocaleString()}</strong></span>
                    </div>
                </div>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuGroup>
                <DropdownMenuItem asChild>
                    <Link
                        className="block w-full"
                        href={edit()}
                        as="button"
                        prefetch
                        onClick={cleanup}
                    >
                        <Settings className="mr-2" />
                        Settings
                    </Link>
                </DropdownMenuItem>
            </DropdownMenuGroup>
            <DropdownMenuSeparator />
            <DropdownMenuItem asChild>
                <Link
                    className="block w-full"
                    href={logout()}
                    as="button"
                    onClick={handleLogout}
                    data-test="logout-button"
                >
                    <LogOut className="mr-2" />
                    Log out
                </Link>
            </DropdownMenuItem>
        </>
    );
}
