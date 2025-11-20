<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Log;
use Symfony\Component\HttpFoundation\Response;

class DebugAuth
{
    public function handle(Request $request, Closure $next): Response
    {
        // Log auth state
        Log::info('Auth Debug', [
            'path' => $request->path(),
            'authenticated' => Auth::check(),
            'user_id' => Auth::id(),
            'session_id' => session('PHPSESSID') ?? session()->getId(),
        ]);

        return $next($request);
    }
}
