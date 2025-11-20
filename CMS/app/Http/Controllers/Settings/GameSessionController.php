<?php

namespace App\Http\Controllers\Settings;

use App\Http\Controllers\Controller;
use App\Rules\CurrentPassword;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\DB;
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
        
        /**
         * Force terminate the game session through database state change
         * 
         * How the game server handles this:
         * 1. The TeraServer reads loggedin field when processing client packets
         * 2. Setting loggedin=0 and SessionIP=null indicates no active session
         * 3. The game server's MapleClient.updateLoginState() method synchronizes this state
         * 4. If a client is currently connected, the server will detect the state change
         *    on the next packet it receives from that client and disconnect them
         * 5. The account becomes available for a new login immediately
         * 
         * This works by leveraging the game server's built-in session validation mechanism
         * rather than requiring direct socket communication with the game server.
         */
        $user->loggedin = 0;
        $user->SessionIP = null;
        $user->save();

        return back()->with('success', 'Game session has been terminated. You can now log back into the game.');
    }
}

