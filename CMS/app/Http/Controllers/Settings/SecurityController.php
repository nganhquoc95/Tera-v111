<?php

namespace App\Http\Controllers\Settings;

use App\Http\Controllers\Controller;
use App\Rules\CurrentPassword;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Inertia\Inertia;

class SecurityController extends Controller
{
    public function edit()
    {
        $user = Auth::user();
        return Inertia::render('settings/security', [
            'picEnabled' => $user->PicEnabled ?? false,
            'has2ndPassword' => !empty($user->{'2ndpassword'}),
        ]);
    }

    public function update(Request $request)
    {
        $validated = $request->validate([
            'pic_enabled' => 'required|boolean',
        ]);

        $user = Auth::user();
        $user->PicEnabled = $validated['pic_enabled'];
        $user->save();

        return back()->with('success', 'Security settings updated successfully.');
    }

    public function reset2ndPassword(Request $request)
    {
        $validated = $request->validate([
            'password' => ['required', new CurrentPassword()],
        ]);

        $user = Auth::user();
        $user->{'2ndpassword'} = null;
        $user->salt2 = null;
        $user->save();

        return back()->with('success', '2nd password has been reset successfully.');
    }
}
