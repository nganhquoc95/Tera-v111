import { Head, Link } from '@inertiajs/react';
import { formatDate } from '@/lib/dateUtils';

interface Account {
    id: number;
    name: string;
    email: string;
    banned: number;
    gm: number;
    createdat: string;
    lastlogin: string;
    loggedin: number;
    NxPrepaid: number;
    NxCredit: number;
    points: number;
    vpoints: number;
}

interface Props {
    account: Account;
}

export default function Show({ account }: Props) {
    const handleDelete = () => {
        if (confirm(`Are you sure you want to delete account "${account.name}"?`)) {
            fetch(`/accounts/${account.id}`, {
                method: 'DELETE',
                headers: {
                    'X-CSRF-TOKEN': document.querySelector('meta[name="csrf-token"]')?.getAttribute('content') || '',
                },
            }).then(() => {
                window.location.href = '/accounts';
            });
        }
    };

    const toggleBan = () => {
        const newBanStatus = account.banned ? 0 : 1;
        fetch(`/accounts/${account.id}/ban`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': document.querySelector('meta[name="csrf-token"]')?.getAttribute('content') || '',
            },
            body: JSON.stringify({ banned: newBanStatus }),
        }).then(() => {
            window.location.reload();
        });
    };

    return (
        <>
            <Head title={`Account: ${account.name}`} />
            <div className="py-12">
                <div className="max-w-4xl mx-auto sm:px-6 lg:px-8">
                    <div className="bg-white overflow-hidden shadow-sm sm:rounded-lg">
                        <div className="p-6 text-gray-900">
                            <div className="flex justify-between items-start mb-6">
                                <div>
                                    <h2 className="text-2xl font-bold">{account.name}</h2>
                                    <p className="text-gray-600 text-sm mt-1">Account ID: {account.id}</p>
                                </div>
                                <div className="flex gap-2">
                                    <Link
                                        href={`/accounts/${account.id}/edit`}
                                        className="bg-green-500 hover:bg-green-700 text-white font-bold py-2 px-4 rounded"
                                    >
                                        Edit
                                    </Link>
                                    <button
                                        onClick={toggleBan}
                                        className={`text-white font-bold py-2 px-4 rounded ${
                                            account.banned
                                                ? 'bg-yellow-500 hover:bg-yellow-700'
                                                : 'bg-orange-500 hover:bg-orange-700'
                                        }`}
                                    >
                                        {account.banned ? 'Unban' : 'Ban'}
                                    </button>
                                    <button
                                        onClick={handleDelete}
                                        className="bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4 rounded"
                                    >
                                        Delete
                                    </button>
                                </div>
                            </div>

                            <div className="grid grid-cols-2 gap-6 mb-6">
                                <div>
                                    <h3 className="text-sm font-semibold text-gray-600 mb-4">Account Information</h3>
                                    <div className="space-y-3">
                                        <div>
                                            <p className="text-xs text-gray-500 uppercase">Email</p>
                                            <p className="text-lg text-gray-900">{account.email || 'Not set'}</p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-gray-500 uppercase">Status</p>
                                            <div className="flex items-center gap-2 mt-1">
                                                <span
                                                    className={`px-3 py-1 rounded text-sm font-semibold ${
                                                        account.banned
                                                            ? 'bg-red-100 text-red-800'
                                                            : 'bg-green-100 text-green-800'
                                                    }`}
                                                >
                                                    {account.banned ? 'BANNED' : 'ACTIVE'}
                                                </span>
                                                {account.gm && (
                                                    <span className="px-3 py-1 rounded text-sm font-semibold bg-yellow-100 text-yellow-800">
                                                        GM
                                                    </span>
                                                )}
                                            </div>
                                        </div>
                                        <div>
                                            <p className="text-xs text-gray-500 uppercase">Online Status</p>
                                            <p className="text-lg text-gray-900">
                                                {account.loggedin ? (
                                                    <span className="text-green-600 font-semibold">Online</span>
                                                ) : (
                                                    <span className="text-gray-500">Offline</span>
                                                )}
                                            </p>
                                        </div>
                                    </div>
                                </div>

                                <div>
                                    <h3 className="text-sm font-semibold text-gray-600 mb-4">Timestamps</h3>
                                    <div className="space-y-3">
                                        <div>
                                            <p className="text-xs text-gray-500 uppercase">Created</p>
                                            <p className="text-sm text-gray-900">{formatDate(account.createdat)}</p>
                                        </div>
                                        <div>
                                            <p className="text-xs text-gray-500 uppercase">Last Login</p>
                                            <p className="text-sm text-gray-900">
                                                {account.lastlogin ? formatDate(account.lastlogin) : 'Never'}
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div className="bg-gray-50 rounded-lg p-4 mb-6">
                                <h3 className="text-sm font-semibold text-gray-600 mb-4">Currency & Points</h3>
                                <div className="grid grid-cols-2 gap-4">
                                    <div className="bg-white rounded p-3">
                                        <p className="text-xs text-gray-500 uppercase">NX Prepaid</p>
                                        <p className="text-2xl font-bold text-gray-900">{account.NxPrepaid}</p>
                                    </div>
                                    <div className="bg-white rounded p-3">
                                        <p className="text-xs text-gray-500 uppercase">NX Credit</p>
                                        <p className="text-2xl font-bold text-gray-900">{account.NxCredit}</p>
                                    </div>
                                    <div className="bg-white rounded p-3">
                                        <p className="text-xs text-gray-500 uppercase">Points</p>
                                        <p className="text-2xl font-bold text-gray-900">{account.points}</p>
                                    </div>
                                    <div className="bg-white rounded p-3">
                                        <p className="text-xs text-gray-500 uppercase">Vote Points</p>
                                        <p className="text-2xl font-bold text-gray-900">{account.vpoints}</p>
                                    </div>
                                </div>
                            </div>

                            <div className="flex gap-4">
                                <Link
                                    href="/accounts"
                                    className="bg-gray-500 hover:bg-gray-700 text-white font-bold py-2 px-4 rounded"
                                >
                                    Back to List
                                </Link>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </>
    );
}
