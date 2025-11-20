import { useState } from 'react';
import { Head, Link, useForm } from '@inertiajs/react';
import { formatDate } from '@/lib/dateUtils';

interface Account {
    id: number;
    name: string;
    email: string;
    banned: number;
    gm: number;
    createdat: string;
    lastlogin: string;
}

interface Props {
    accounts: Account[];
}

export default function Index({ accounts }: Props) {
    const [searchQuery, setSearchQuery] = useState('');
    const [selectedIds, setSelectedIds] = useState<number[]>([]);

    const filteredAccounts = accounts.filter(account =>
        account.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (account.email && account.email.toLowerCase().includes(searchQuery.toLowerCase()))
    );

    const toggleSelectAll = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.checked) {
            setSelectedIds(filteredAccounts.map(a => a.id));
        } else {
            setSelectedIds([]);
        }
    };

    const toggleSelect = (id: number) => {
        setSelectedIds(prev =>
            prev.includes(id) ? prev.filter(i => i !== id) : [...prev, id]
        );
    };

    const handleBulkDelete = () => {
        if (selectedIds.length === 0) {
            alert('No accounts selected');
            return;
        }

        if (confirm(`Delete ${selectedIds.length} account(s)?`)) {
            fetch('/accounts/bulk-delete', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': document.querySelector('meta[name="csrf-token"]')?.getAttribute('content') || '',
                },
                body: JSON.stringify({ ids: selectedIds }),
            }).then(() => {
                window.location.reload();
            });
        }
    };

    const getBanStatus = (banned: number) => {
        return banned ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800';
    };

    const getBanText = (banned: number) => {
        return banned ? 'BANNED' : 'ACTIVE';
    };

    return (
        <>
            <Head title="Manage Accounts" />
            <div className="py-12">
                <div className="max-w-7xl mx-auto sm:px-6 lg:px-8">
                    <div className="bg-white overflow-hidden shadow-sm sm:rounded-lg">
                        <div className="p-6 text-gray-900">
                            <div className="flex justify-between items-center mb-6">
                                <h2 className="text-2xl font-bold">Accounts Management</h2>
                                <Link
                                    href="/accounts/create"
                                    className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
                                >
                                    Create Account
                                </Link>
                            </div>

                            <div className="mb-6 flex gap-4">
                                <input
                                    type="text"
                                    placeholder="Search by name or email..."
                                    value={searchQuery}
                                    onChange={(e) => setSearchQuery(e.target.value)}
                                    className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                />
                                {selectedIds.length > 0 && (
                                    <button
                                        onClick={handleBulkDelete}
                                        className="bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4 rounded"
                                    >
                                        Delete Selected ({selectedIds.length})
                                    </button>
                                )}
                            </div>

                            <div className="overflow-x-auto">
                                <table className="min-w-full border-collapse border border-gray-300">
                                    <thead className="bg-gray-100">
                                        <tr>
                                            <th className="border border-gray-300 px-4 py-2">
                                                <input
                                                    type="checkbox"
                                                    checked={selectedIds.length === filteredAccounts.length && filteredAccounts.length > 0}
                                                    onChange={toggleSelectAll}
                                                />
                                            </th>
                                            <th className="border border-gray-300 px-4 py-2 text-left">Account Name</th>
                                            <th className="border border-gray-300 px-4 py-2 text-left">Email</th>
                                            <th className="border border-gray-300 px-4 py-2 text-left">Status</th>
                                            <th className="border border-gray-300 px-4 py-2 text-left">GM</th>
                                            <th className="border border-gray-300 px-4 py-2 text-left">Created</th>
                                            <th className="border border-gray-300 px-4 py-2 text-left">Last Login</th>
                                            <th className="border border-gray-300 px-4 py-2 text-left">Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {filteredAccounts.length > 0 ? (
                                            filteredAccounts.map((account) => (
                                                <tr key={account.id} className="hover:bg-gray-50">
                                                    <td className="border border-gray-300 px-4 py-2">
                                                        <input
                                                            type="checkbox"
                                                            checked={selectedIds.includes(account.id)}
                                                            onChange={() => toggleSelect(account.id)}
                                                        />
                                                    </td>
                                                    <td className="border border-gray-300 px-4 py-2 font-semibold">{account.name}</td>
                                                    <td className="border border-gray-300 px-4 py-2">{account.email || '-'}</td>
                                                    <td className="border border-gray-300 px-4 py-2">
                                                        <span className={`px-3 py-1 rounded text-sm font-semibold ${getBanStatus(account.banned)}`}>
                                                            {getBanText(account.banned)}
                                                        </span>
                                                    </td>
                                                    <td className="border border-gray-300 px-4 py-2 text-center">
                                                        {account.gm ? (
                                                            <span className="px-2 py-1 bg-yellow-100 text-yellow-800 rounded text-sm font-semibold">
                                                                GM
                                                            </span>
                                                        ) : (
                                                            <span className="text-gray-500">-</span>
                                                        )}
                                                    </td>
                                                    <td className="border border-gray-300 px-4 py-2 text-sm">{formatDate(account.createdat)}</td>
                                                    <td className="border border-gray-300 px-4 py-2 text-sm">
                                                        {account.lastlogin ? formatDate(account.lastlogin) : 'Never'}
                                                    </td>
                                                    <td className="border border-gray-300 px-4 py-2 space-x-2">
                                                        <Link
                                                            href={`/accounts/${account.id}`}
                                                            className="text-blue-500 hover:text-blue-700 font-semibold text-sm"
                                                        >
                                                            View
                                                        </Link>
                                                        <Link
                                                            href={`/accounts/${account.id}/edit`}
                                                            className="text-green-500 hover:text-green-700 font-semibold text-sm"
                                                        >
                                                            Edit
                                                        </Link>
                                                    </td>
                                                </tr>
                                            ))
                                        ) : (
                                            <tr>
                                                <td colSpan={8} className="border border-gray-300 px-4 py-2 text-center text-gray-500">
                                                    No accounts found
                                                </td>
                                            </tr>
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </>
    );
}
