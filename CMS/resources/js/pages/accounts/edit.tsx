import { FormEventHandler } from 'react';
import { Head, Link, useForm } from '@inertiajs/react';

interface Account {
    id: number;
    name: string;
    email: string;
    gender: number;
    banned: number;
}

interface Props {
    account: Account;
}

export default function Edit({ account }: Props) {
    const { data, setData, patch, errors } = useForm({
        name: account.name,
        email: account.email || '',
        password: '',
        password_confirmation: '',
        gender: String(account.gender),
    });

    const submit: FormEventHandler = (e) => {
        e.preventDefault();
        patch(`/accounts/${account.id}`);
    };

    return (
        <>
            <Head title={`Edit Account: ${account.name}`} />
            <div className="py-12">
                <div className="max-w-2xl mx-auto sm:px-6 lg:px-8">
                    <div className="bg-white overflow-hidden shadow-sm sm:rounded-lg">
                        <div className="p-6 text-gray-900">
                            <h2 className="text-2xl font-bold mb-6">Edit Account: {account.name}</h2>

                            <form onSubmit={submit} className="space-y-6">
                                <div>
                                    <label htmlFor="name" className="block text-sm font-medium text-gray-700">
                                        Account Name
                                    </label>
                                    <input
                                        id="name"
                                        type="text"
                                        value={data.name}
                                        onChange={(e) => setData('name', e.currentTarget.value)}
                                        className="mt-1 block w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                    {errors.name && (
                                        <p className="mt-1 text-sm text-red-500">{errors.name}</p>
                                    )}
                                </div>

                                <div>
                                    <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                                        Email
                                    </label>
                                    <input
                                        id="email"
                                        type="email"
                                        value={data.email}
                                        onChange={(e) => setData('email', e.currentTarget.value)}
                                        className="mt-1 block w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                    {errors.email && (
                                        <p className="mt-1 text-sm text-red-500">{errors.email}</p>
                                    )}
                                </div>

                                <div>
                                    <label htmlFor="gender" className="block text-sm font-medium text-gray-700">
                                        Gender
                                    </label>
                                    <select
                                        id="gender"
                                        value={data.gender}
                                        onChange={(e) => setData('gender', e.currentTarget.value)}
                                        className="mt-1 block w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    >
                                        <option value="0">Male</option>
                                        <option value="1">Female</option>
                                    </select>
                                    {errors.gender && (
                                        <p className="mt-1 text-sm text-red-500">{errors.gender}</p>
                                    )}
                                </div>

                                <div className="bg-blue-50 border border-blue-200 rounded p-4">
                                    <p className="text-sm text-blue-800 mb-4">Leave password fields empty to keep current password</p>

                                    <div className="space-y-4">
                                        <div>
                                            <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                                                New Password
                                            </label>
                                            <input
                                                id="password"
                                                type="password"
                                                value={data.password}
                                                onChange={(e) => setData('password', e.currentTarget.value)}
                                                className="mt-1 block w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                            />
                                            {errors.password && (
                                                <p className="mt-1 text-sm text-red-500">{errors.password}</p>
                                            )}
                                        </div>

                                        <div>
                                            <label htmlFor="password_confirmation" className="block text-sm font-medium text-gray-700">
                                                Confirm Password
                                            </label>
                                            <input
                                                id="password_confirmation"
                                                type="password"
                                                value={data.password_confirmation}
                                                onChange={(e) => setData('password_confirmation', e.currentTarget.value)}
                                                className="mt-1 block w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                                            />
                                        </div>
                                    </div>
                                </div>

                                <div className="flex gap-4">
                                    <button
                                        type="submit"
                                        className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
                                    >
                                        Update Account
                                    </button>
                                    <Link
                                        href={`/accounts/${account.id}`}
                                        className="bg-gray-500 hover:bg-gray-700 text-white font-bold py-2 px-4 rounded"
                                    >
                                        Cancel
                                    </Link>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </>
    );
}
