<?php

namespace App\Http\Controllers;

use App\Models\Account;
use App\Http\Requests\StoreAccountRequest;
use App\Http\Requests\UpdateAccountRequest;
use Illuminate\Http\Request;
use Inertia\Inertia;

class AccountController extends Controller
{
    /**
     * Display a listing of accounts.
     */
    public function index()
    {
        $accounts = Account::all();
        
        return Inertia::render('accounts/index', [
            'accounts' => $accounts,
        ]);
    }

    /**
     * Show the form for creating a new account.
     */
    public function create()
    {
        return Inertia::render('accounts/create');
    }

    /**
     * Store a newly created account in storage.
     */
    public function store(StoreAccountRequest $request)
    {
        $validated = $request->validated();
        
        // Password is automatically hashed via the model's setPasswordAttribute
        $account = Account::create($validated);

        return redirect()->route('accounts.show', $account->id)
                        ->with('success', 'Account created successfully');
    }

    /**
     * Display the specified account.
     */
    public function show(Account $account)
    {
        return Inertia::render('accounts/show', [
            'account' => $account,
        ]);
    }

    /**
     * Show the form for editing the specified account.
     */
    public function edit(Account $account)
    {
        return Inertia::render('accounts/edit', [
            'account' => $account,
        ]);
    }

    /**
     * Update the specified account in storage.
     */
    public function update(UpdateAccountRequest $request, Account $account)
    {
        $validated = $request->validated();

        // Only hash password if it's being updated
        if (isset($validated['password'])) {
            $validated['password'] = Account::hashPassword($validated['password']);
        }

        $account->update($validated);

        return redirect()->route('accounts.show', $account->id)
                        ->with('success', 'Account updated successfully');
    }

    /**
     * Remove the specified account from storage.
     */
    public function destroy(Account $account)
    {
        $accountName = $account->name;
        $account->delete();

        return redirect()->route('accounts.index')
                        ->with('success', "Account '{$accountName}' deleted successfully");
    }

    /**
     * Search accounts by name or email.
     */
    public function search(Request $request)
    {
        $query = $request->input('query');
        
        if (!$query) {
            return response()->json([]);
        }

        $accounts = Account::where('name', 'like', "%{$query}%")
                           ->orWhere('email', 'like', "%{$query}%")
                           ->get();

        return response()->json($accounts);
    }

    /**
     * Bulk delete accounts.
     */
    public function bulkDelete(Request $request)
    {
        $ids = $request->input('ids', []);
        
        if (empty($ids)) {
            return response()->json(['error' => 'No accounts selected'], 422);
        }

        $count = Account::whereIn('id', $ids)->delete();

        return response()->json(['message' => $count . ' account(s) deleted successfully']);
    }

    /**
     * Toggle account ban status.
     */
    public function toggleBan(Request $request, Account $account)
    {
        $request->validate([
            'banned' => 'required|boolean',
            'banreason' => 'nullable|string',
        ]);

        $account->update([
            'banned' => $request->input('banned'),
            'banreason' => $request->input('banreason'),
        ]);

        return response()->json(['message' => 'Ban status updated successfully']);
    }

    /**
     * Set temporary ban on account.
     */
    public function setTempBan(Request $request, Account $account)
    {
        $request->validate([
            'tempban' => 'required|date',
        ]);

        $account->update(['tempban' => $request->input('tempban')]);

        return response()->json(['message' => 'Temporary ban set successfully']);
    }

    /**
     * Get account statistics.
     */
    public function stats()
    {
        $totalAccounts = Account::count();
        $bannedAccounts = Account::where('banned', 1)->count();
        $gmAccounts = Account::where('gm', 1)->count();
        $loggedInAccounts = Account::where('loggedin', 1)->count();

        return response()->json([
            'total' => $totalAccounts,
            'banned' => $bannedAccounts,
            'gm' => $gmAccounts,
            'loggedIn' => $loggedInAccounts,
        ]);
    }
}
