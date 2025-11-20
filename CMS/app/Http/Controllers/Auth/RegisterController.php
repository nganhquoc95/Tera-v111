<?php

namespace App\Http\Controllers\Auth;

use App\Http\Controllers\Controller;
use App\Models\Account;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Validation\Rules\Password;
use Inertia\Inertia;

class RegisterController extends Controller
{
    public function show()
    {
        return Inertia::render('auth/register');
    }

    public function register(Request $request)
    {
        $validated = $request->validate([
            'name' => 'required|string|min:4|max:14|unique:accounts,name',
            'email' => 'required|email|unique:accounts,email',
            'password' => [
                'required',
                'string',
                'min:8',
                'confirmed',
                Password::min(8)
                    ->mixedCase()
                    ->numbers()
                    ->symbols(),
            ],
        ], [
            'name.required' => 'Account name is required.',
            'name.min' => 'Account name must be at least 4 characters.',
            'name.max' => 'Account name must not exceed 14 characters.',
            'name.unique' => 'This account name is already taken.',
            'email.required' => 'Email address is required.',
            'email.email' => 'Email must be a valid email address.',
            'email.unique' => 'This email is already registered.',
            'password.required' => 'Password is required.',
            'password.min' => 'Password must be at least 8 characters.',
            'password.confirmed' => 'Passwords do not match.',
        ]);

        // Create new account
        $account = Account::create([
            'name' => $validated['name'],
            'email' => $validated['email'],
            'password' => $validated['password'], // Will be hashed by Account model
            'birthday' => '2000-01-01', // Default birthday
            'loggedIn' => 0,
            'lastlogin' => now()->format('Y-m-d H:i:s'),
            'created_at' => now(),
            'updated_at' => now(),
        ]);

        // Log the user in
        Auth::guard('web')->login($account);

        return redirect()->route('dashboard')
            ->with('message', 'Account created successfully! Welcome to MapleStory.');
    }
}
