<?php

namespace App\Http\Controllers\Auth;

use App\Http\Controllers\Controller;
use App\Mail\PasswordResetMail;
use App\Models\Account;
use App\Models\PasswordReset;
use App\Services\EmailPreviewService;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Mail;
use Illuminate\Validation\Rules\Password;
use Inertia\Inertia;

class PasswordResetController extends Controller
{
    public function showRequestForm()
    {
        return Inertia::render('auth/forgot-password');
    }

    public function sendResetLink(Request $request)
    {
        $request->validate([
            'name' => 'required|string|exists:accounts,name',
            'email' => 'required|email|exists:accounts,email',
        ], [
            'name.exists' => 'The provided account name does not exist.',
            'email.exists' => 'The email does not match our records.',
        ]);

        $account = Account::where('name', $request->name)
            ->where('email', $request->email)
            ->first();

        if (!$account) {
            return back()->withErrors([
                'email' => 'The account name and email do not match.',
            ]);
        }

        // Generate confirmation key
        $confirmkey = bin2hex(random_bytes(32));

        // Create password reset record
        $reset = PasswordReset::updateOrCreate(
            ['name' => $account->name],
            [
                'id' => $account->id,
                'email' => $account->email,
                'confirmkey' => $confirmkey,
                'status' => 0,
                'timestamp' => (string) now()->getTimestamp(),
            ]
        );

        // Send password reset email
        try {
            Mail::send(new PasswordResetMail($reset, $account));
            Log::info("Password reset email sent to: {$account->email}");
        } catch (\Exception $e) {
            Log::error("Failed to send password reset email: " . $e->getMessage());
        }

        // In development, generate and open HTML preview
        $emailPreviewService = new EmailPreviewService();
        $emailPreviewService->generateAndOpenPasswordResetPreview($reset, $account);

        return redirect()->route('password.reset.form', ['key' => $confirmkey])
            ->with('message', 'Password reset link has been sent to your email address.');
    }

    public function showResetForm(Request $request)
    {
        $key = $request->query('key');

        if (!$key) {
            return redirect()->route('password.request')
                ->withErrors(['key' => 'Reset key is missing.']);
        }

        $reset = PasswordReset::where('confirmkey', $key)->first();

        if (!$reset) {
            return redirect()->route('password.request')
                ->withErrors(['key' => 'Invalid or expired reset key.']);
        }

        return Inertia::render('auth/reset-password', [
            'token' => $key,
            'email' => $reset->email,
        ]);
    }

    public function resetPassword(Request $request)
    {
        $request->validate([
            'token' => 'required|string',
            'password' => ['required', Password::defaults(), 'confirmed'],
        ]);
        
        $reset = PasswordReset::where('confirmkey', $request->token)->first();

        if (!$reset) {
            return back()->withErrors(['token' => 'Invalid or expired reset token.']);
        }

        $account = Account::find($reset->id);

        if (!$account) {
            return back()->withErrors(['token' => 'Account not found.']);
        }

        // Update password using bcrypt
        $account->password = $request->password;
        $account->save();

        // Mark reset as used
        $reset->status = 1;
        $reset->save();

        return redirect()->route('login')
            ->with('message', 'Password reset successfully. Please log in with your new password.');
    }
}
