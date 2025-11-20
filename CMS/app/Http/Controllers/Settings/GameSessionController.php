<?php

namespace App\Http\Controllers\Settings;

use App\Http\Controllers\Controller;
use App\Rules\CurrentPassword;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Inertia\Inertia;

class GameSessionController extends Controller
{
    public function edit()
    {
        $user = Auth::user();
        return Inertia::render('settings/game-session', [
            'isLoggedInGame' => $user->loggedin >= 1,
            'lastLogin' => $user->lastlogin,
            'sessionIP' => $user->SessionIP,
        ]);
    }

    public function forceLogout(Request $request)
    {
        $validated = $request->validate([
            'password' => ['required', new CurrentPassword()],
        ]);

        $user = Auth::user();

        $user->loggedin = 0;
        $user->SessionIP = null;
        $user->save();

        return back()->with('success', 'Game session has been terminated. You can now log back into the game.');
    }
}

