<?php

namespace App\Http\Controllers\Auth;

use App\Http\Controllers\Controller;
use App\Models\Account;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Log;
use Inertia\Inertia;

class LoginController extends Controller
{
    public function show()
    {
        return Inertia::render('auth/login');
    }

    public function authenticate(Request $request)
    {
        $credentials = $request->validate([
            'name' => 'required|string',
            'password' => 'required|string',
        ]);

        // Find account by name
        $account = Account::where('name', $credentials['name'])->first();

        if (!$account) {
            return back()->withErrors([
                'name' => 'The provided name does not exist.',
            ])->onlyInput('name');
        }

        // Verify password using SHA-512 with salt
        $hashedPassword = hash('sha512', $credentials['password'] . $account->salt);

        if ($hashedPassword !== $account->password) {
            return back()->withErrors([
                'password' => 'The provided password is incorrect.',
            ])->onlyInput('name');
        }

        // Log the user in
        Auth::guard('web')->login($account, $request->boolean('remember'));
        
        return redirect()->intended('dashboard');
    }
}
