<?php

use App\Http\Controllers\Settings\GameSessionController;
use App\Http\Controllers\Settings\PasswordController;
use App\Http\Controllers\Settings\ProfileController;
use App\Http\Controllers\Settings\SecurityController;
use Illuminate\Support\Facades\Route;
use Inertia\Inertia;

Route::middleware('auth')->group(function () {
    Route::redirect('settings', '/settings/profile');

    Route::get('settings/profile', [ProfileController::class, 'edit'])->name('profile.edit');
    Route::patch('settings/profile', [ProfileController::class, 'update'])->name('profile.update');
    Route::delete('settings/profile', [ProfileController::class, 'destroy'])->name('profile.destroy');

    Route::get('settings/password', [PasswordController::class, 'edit'])->name('user-password.edit');

    Route::put('settings/password', [PasswordController::class, 'update'])
        ->middleware('throttle:6,1')
        ->name('user-password.update');

    Route::get('settings/security', [SecurityController::class, 'edit'])->name('security.edit');
    Route::patch('settings/security', [SecurityController::class, 'update'])->name('security.update');
    Route::post('settings/security/reset-2nd-password', [SecurityController::class, 'reset2ndPassword'])->name('security.reset-2nd-password');

    Route::get('settings/game-session', [GameSessionController::class, 'edit'])->name('game-session.edit');
    Route::post('settings/game-session/force-logout', [GameSessionController::class, 'forceLogout'])->name('game-session.force-logout');

    Route::get('settings/appearance', function () {
        return Inertia::render('settings/appearance');
    })->name('appearance.edit');
});
